package de.batteryng.mvp.usage

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Small local time series. Every sample is a cumulative, on-battery mAh value per UID. */
internal class UsageHistoryStore(private val context: Context) {
    private val file = File(context.filesDir, "battery_usage_history_v1.json")

    fun latestSampleAt(): Long = read().lastOrNull()?.at ?: 0L

    fun append(
        byUidMah: Map<Int, Double>,
        now: Long = System.currentTimeMillis(),
        windowStartAt: Long = now - UsageHistoryMath.WINDOW_MS,
        strictStartBoundary: Boolean = false,
    ): UsageSummary {
        val snapshots = read().toMutableList()
        val previous = snapshots.lastOrNull()
        if (previous != null && now <= previous.at) {
            // Wall clock changed backwards; old timestamps cannot describe the next 24 hours.
            snapshots.clear()
        }

        val last = snapshots.lastOrNull()
        val reset = last != null && UsageHistoryMath.isReset(last.byUidMah, byUidMah)
        val reboot = last != null && SystemClock.elapsedRealtime() < last.elapsed
        snapshots.add(
            UsageSnapshot(
                at = now,
                elapsed = SystemClock.elapsedRealtime(),
                byUidMah = byUidMah,
                resetBefore = reset,
                rebootBefore = reboot,
            )
        )
        prune(snapshots, now)
        write(snapshots)
        return UsageHistoryMath.summarize(snapshots, now, windowStartAt, strictStartBoundary)
    }

    fun summary(
        now: Long = System.currentTimeMillis(),
        windowStartAt: Long = now - UsageHistoryMath.WINDOW_MS,
        strictStartBoundary: Boolean = false,
    ): UsageSummary = UsageHistoryMath.summarize(read(), now, windowStartAt, strictStartBoundary)

    private fun read(): List<UsageSnapshot> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val entry = array.optJSONObject(index) ?: continue
                    val usage = entry.optJSONObject("u") ?: continue
                    val byUid = mutableMapOf<Int, Double>()
                    val keys = usage.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val uid = key.toIntOrNull() ?: continue
                        val mah = usage.optDouble(key, Double.NaN)
                        if (mah.isFinite() && mah >= 0.0) byUid[uid] = mah
                    }
                    add(
                        UsageSnapshot(
                            at = entry.optLong("t"),
                            elapsed = entry.optLong("e"),
                            byUidMah = byUid,
                            resetBefore = entry.optBoolean("r"),
                            rebootBefore = entry.optBoolean("b"),
                        )
                    )
                }
            }.filter { it.at > 0L }.sortedBy { it.at }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun write(snapshots: List<UsageSnapshot>) {
        val array = JSONArray()
        snapshots.forEach { snapshot ->
            val usage = JSONObject()
            snapshot.byUidMah.forEach { (uid, mah) -> usage.put(uid.toString(), mah) }
            array.put(
                JSONObject()
                    .put("t", snapshot.at)
                    .put("e", snapshot.elapsed)
                    .put("r", snapshot.resetBefore)
                    .put("b", snapshot.rebootBefore)
                    .put("u", usage)
            )
        }
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(file)) {
            // Some filesystems cannot atomically replace an existing target.
            file.writeText(array.toString())
            temp.delete()
        }
    }

    private fun prune(snapshots: MutableList<UsageSnapshot>, now: Long) {
        val chargeAt = UsageChargeTracker.lastFullChargeAt(context)
        val cutoff = if (chargeAt > 0L) {
            minOf(now - UsageHistoryMath.WINDOW_MS, chargeAt)
        } else now - UsageHistoryMath.WINDOW_MS
        val firstInWindow = snapshots.indexOfFirst { it.at >= cutoff }
        if (firstInWindow > 1) snapshots.subList(0, firstInWindow - 1).clear()
        if (firstInWindow == -1 && snapshots.size > 1) {
            snapshots.subList(0, snapshots.size - 1).clear()
        }
    }

}
