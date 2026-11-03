package de.batteryng.mvp.usage

import android.Manifest
import android.app.AppOpsManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import de.batteryng.mvp.AppAppearance
import de.batteryng.mvp.R
import java.io.InputStreamReader
import java.io.IOException
import java.io.Reader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class UsageItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val estimatedMah: Double,
)

sealed interface UsageState {
    data object Loading : UsageState
    data object PermissionRequired : UsageState
    data object Collecting : UsageState
    data object AwaitingFullCharge : UsageState
    data object Empty : UsageState
    data object Error : UsageState
    data class Data(
        val items: List<UsageItem>,
        val coverageHours: Double = 0.0,
        val hasGaps: Boolean = false,
        val updatedAt: Long = 0L,
        val period: UsagePeriod = UsagePeriod.LAST_24_HOURS,
        val topCount: Int = 3,
        val windowStartAt: Long = 0L,
    ) : UsageState
}

/** Collects and ranks per-UID energy estimates over the selected time window. */
object UsageTopThreeRepository {
    private const val JOB_ID = 0x424e4701
    private const val SAMPLE_INTERVAL_MS = 15L * 60L * 1000L
    private const val MIN_SAMPLE_SPACING_MS = 5L * 60L * 1000L
    private const val PROCESS_TIMEOUT_MS = 20_000L

    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "battery-usage-collector").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var packageCacheAt = 0L
    private var packageCache: Map<Int, List<String>> = emptyMap()

    /** Calls back on the main thread. A first sample establishes a baseline and returns Collecting. */
    fun load(context: Context, callback: (UsageState) -> Unit) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) callback(UsageState.Loading)
        else mainHandler.post { callback(UsageState.Loading) }
        schedule(appContext)
        executor.execute {
            val state = try {
                val settings = UsagePreferences.load(appContext)
                val observation = UsageChargeTracker.observe(appContext)
                if (!hasAccess(appContext)) UsageState.PermissionRequired
                else {
                    val store = UsageHistoryStore(appContext)
                    val now = System.currentTimeMillis()
                    val windowStartAt = windowStartAt(now, settings.period, observation.lastFullChargeAt)
                    val strictStartBoundary = settings.period == UsagePeriod.SINCE_FULL_CHARGE
                    val shouldSample = observation.newlyQualified ||
                        store.latestSampleAt() < observation.lastFullChargeAt ||
                        now - store.latestSampleAt() >= MIN_SAMPLE_SPACING_MS
                    val summary = if (shouldSample) {
                        val parsed = readAccumulatedBatteryUsage()
                        when {
                            parsed.permissionDenied -> {
                                mainHandler.post { callback(UsageState.PermissionRequired) }
                                return@execute
                            }
                            !parsed.foundUsage -> {
                                mainHandler.post { callback(UsageState.Error) }
                                return@execute
                            }
                            else -> store.append(
                                parsed.byUidMah,
                                System.currentTimeMillis(),
                                windowStartAt,
                                strictStartBoundary,
                            )
                        }
                    } else store.summary(now, windowStartAt, strictStartBoundary)
                    toState(appContext, summary, settings, windowStartAt)
                }
            } catch (_: Exception) {
                UsageState.Error
            }
            mainHandler.post { callback(state) }
        }
    }

    /** Used by JobScheduler; collection continues while the activity and overlay are closed. */
    internal fun collectFromJob(
        context: Context,
        shouldContinue: () -> Boolean,
        finished: () -> Unit,
    ) {
        val appContext = context.applicationContext
        executor.execute {
            try {
                val observation = UsageChargeTracker.observe(appContext)
                if (shouldContinue() && hasAccess(appContext)) {
                    val store = UsageHistoryStore(appContext)
                    val now = System.currentTimeMillis()
                    if (observation.newlyQualified ||
                        store.latestSampleAt() < observation.lastFullChargeAt ||
                        now - store.latestSampleAt() >= MIN_SAMPLE_SPACING_MS
                    ) {
                        val parsed = readAccumulatedBatteryUsage()
                        if (shouldContinue() && parsed.foundUsage && !parsed.permissionDenied) {
                            store.append(parsed.byUidMah, System.currentTimeMillis())
                        }
                    }
                }
            } catch (_: Exception) {
                // The next periodic run can retry.
            } finally {
                mainHandler.post { finished() }
            }
        }
    }

    /** An immediate baseline near a qualifying full charge, independent of the periodic job. */
    internal fun collectChargeBaseline(
        context: Context,
        chargeAt: Long,
        finished: (() -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        executor.execute {
            try {
                if (!hasAccess(appContext)) return@execute
                val store = UsageHistoryStore(appContext)
                if (store.latestSampleAt() >= chargeAt) return@execute
                val parsed = readAccumulatedBatteryUsage()
                if (parsed.foundUsage && !parsed.permissionDenied) {
                    store.append(parsed.byUidMah, System.currentTimeMillis())
                }
            } catch (_: Exception) {
                // The regular sampler will retry; the UI will show incomplete coverage.
            } finally {
                if (finished != null) mainHandler.post(finished)
            }
        }
    }

    internal fun schedule(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            ?: return
        if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return
        val job = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, UsageSamplingJobService::class.java),
        )
            .setPeriodic(SAMPLE_INTERVAL_MS)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }

    private fun hasAccess(context: Context): Boolean {
        if (context.checkSelfPermission(Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT
    }

    private fun readAccumulatedBatteryUsage(): BatterystatsParser.Result = runDump(
        arguments = listOf("batterystats", "--usage", "--accumulated"),
        parse = BatterystatsParser::parse,
        permissionDenied = { it.permissionDenied },
    )

    private fun readPackageMap(): Map<Int, List<String>> {
        val now = System.currentTimeMillis()
        if (now - packageCacheAt < SAMPLE_INTERVAL_MS && packageCache.isNotEmpty()) {
            return packageCache
        }
        val parsed = runDump(
            arguments = listOf("package", "--checkin", "packages"),
            parse = PackageCheckinParser::parse,
            permissionDenied = { it.permissionDenied },
        )
        if (parsed.permissionDenied || parsed.packagesByUid.isEmpty()) {
            throw IOException("package checkin unavailable")
        }
        packageCache = parsed.packagesByUid
        packageCacheAt = now
        return packageCache
    }

    private fun <T> runDump(
        arguments: List<String>,
        parse: (Reader) -> T,
        permissionDenied: (T) -> Boolean,
    ): T {
        val process = ProcessBuilder(listOf("/system/bin/dumpsys") + arguments)
            .redirectErrorStream(true)
            .start()
        val timedOut = AtomicBoolean(false)
        val watchdog = Thread {
            try {
                Thread.sleep(PROCESS_TIMEOUT_MS)
                timedOut.set(true)
                process.destroy()
            } catch (_: InterruptedException) {
                // Completed before the deadline.
            }
        }.apply {
            isDaemon = true
            start()
        }
        return try {
            val parsed = InputStreamReader(process.inputStream).use(parse)
            val exitCode = process.waitFor()
            if (timedOut.get()) throw IOException("dumpsys timed out")
            if (exitCode != 0 && !permissionDenied(parsed)) {
                throw IOException("dumpsys exited with status $exitCode")
            }
            parsed
        } finally {
            watchdog.interrupt()
            process.destroy()
        }
    }

    private fun windowStartAt(now: Long, period: UsagePeriod, chargeAt: Long): Long =
        if (period == UsagePeriod.SINCE_FULL_CHARGE && chargeAt > 0L) chargeAt
        else now - UsageHistoryMath.WINDOW_MS

    private fun toState(
        context: Context,
        summary: UsageSummary,
        settings: UsageSettings,
        windowStartAt: Long,
    ): UsageState {
        if (settings.period == UsagePeriod.SINCE_FULL_CHARGE &&
            UsageChargeTracker.lastFullChargeAt(context) <= 0L
        ) return UsageState.AwaitingFullCharge
        if (!summary.hasTwoSamples) return UsageState.Collecting
        val ownUser = Process.myUid() / 100_000
        val candidates = summary.byUidMah.entries
            .asSequence()
            .filter { (_, mah) -> mah > 0.0 }
            .sortedByDescending { it.value }
            .take(settings.topCount.coerceIn(UsagePreferences.MIN_TOP_COUNT, UsagePreferences.MAX_TOP_COUNT))
            .toList()
        val packageMap = if (candidates.any { it.key / 100_000 != ownUser }) {
            try { readPackageMap() } catch (_: Exception) { emptyMap() }
        } else emptyMap()
        val localizedContext = AppAppearance.wrap(context)
        val items = candidates.map { (uid, mah) ->
            resolveItem(localizedContext, uid, mah, ownUser, packageMap)
        }
        return UsageState.Data(
            items = items,
            coverageHours = summary.coverageHours,
            hasGaps = summary.hasGaps,
            updatedAt = summary.updatedAt,
            period = settings.period,
            topCount = settings.topCount,
            windowStartAt = windowStartAt,
        )
    }

    private fun resolveItem(
        context: Context,
        uid: Int,
        mah: Double,
        ownUser: Int,
        packageMap: Map<Int, List<String>>,
    ): UsageItem {
        val packageManager = context.packageManager
        val isOtherProfile = uid / 100_000 != ownUser
        val suffix = if (isOtherProfile) {
            " " + context.getString(R.string.top_app_work_profile_suffix)
        } else ""
        val packages = packageMap[uid] ?: if (!isOtherProfile) {
            try {
                packageManager.getPackagesForUid(uid)?.distinct()?.sorted().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()
        if (packages.isEmpty()) {
            val name = if (isOtherProfile) {
                context.getString(R.string.top_app_work_profile_uid, uid)
            } else {
                context.getString(R.string.top_app_uid, uid)
            }
            return UsageItem("uid:$uid", name, null, mah)
        }
        val lookupFlags = if (isOtherProfile) PackageManager.MATCH_UNINSTALLED_PACKAGES else 0
        if (packages.size > 1) {
            val labels = packages.map { packageName ->
                try {
                    val info = packageManager.getApplicationInfo(packageName, lookupFlags)
                    packageManager.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    packageName
                }
            }
            return UsageItem(
                packageName = packages.joinToString(","),
                appName = labels.joinToString(" + ") + suffix,
                icon = null,
                estimatedMah = mah,
            )
        }
        val packageName = packages.single()
        return try {
            val info = packageManager.getApplicationInfo(packageName, lookupFlags)
            UsageItem(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(info).toString() + suffix,
                icon = packageManager.getApplicationIcon(info),
                estimatedMah = max(0.0, mah),
            )
        } catch (_: Exception) {
            UsageItem(packageName, packageName + suffix, null, mah)
        }
    }
}
