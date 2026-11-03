package de.batteryng.mvp.usage

import kotlin.math.max
import kotlin.math.min

internal data class UsageSnapshot(
    val at: Long,
    val elapsed: Long,
    val byUidMah: Map<Int, Double>,
    val resetBefore: Boolean = false,
    val rebootBefore: Boolean = false,
)

internal data class UsageSummary(
    val byUidMah: Map<Int, Double>,
    val coverageHours: Double,
    val hasGaps: Boolean,
    val updatedAt: Long,
    val hasTwoSamples: Boolean,
)

/** Pure window calculation, independent of Android I/O and the charging cycle. */
internal object UsageHistoryMath {
    const val WINDOW_MS = 24L * 60L * 60L * 1000L
    const val GAP_THRESHOLD_MS = 45L * 60L * 1000L
    private const val HOUR_MS = 60 * 60 * 1000.0
    private const val ROUNDING_TOLERANCE_MAH = 0.002

    fun summarize(
        snapshots: List<UsageSnapshot>,
        now: Long,
        windowStartAt: Long = now - WINDOW_MS,
        strictStartBoundary: Boolean = false,
    ): UsageSummary {
        val cutoff = windowStartAt.coerceAtMost(now)
        val totals = mutableMapOf<Int, Double>()
        var coveredMs = 0L
        var hasGaps = false
        var observedIntervals = 0

        for (index in 1 until snapshots.size) {
            val before = snapshots[index - 1]
            val after = snapshots[index]
            if (after.at <= cutoff || before.at >= now) continue
            val intervalMs = after.at - before.at
            if (intervalMs <= 0L) continue
            val overlapMs = max(0L, min(now, after.at) - max(cutoff, before.at))
            if (overlapMs == 0L) continue
            observedIntervals++
            if (strictStartBoundary && before.at < cutoff && after.at > cutoff) {
                // A charge boundary cannot safely share an interval with pre-charge drain.
                hasGaps = true
                continue
            }
            if (after.resetBefore || intervalMs > GAP_THRESHOLD_MS) {
                hasGaps = true
                continue
            }
            coveredMs += overlapMs
            if (after.rebootBefore) hasGaps = true
            val fraction = overlapMs.toDouble() / intervalMs
            for ((uid, current) in after.byUidMah) {
                // A first-seen UID has no trustworthy baseline; count it from the next sample.
                val past = before.byUidMah[uid] ?: continue
                if (current + ROUNDING_TOLERANCE_MAH < past) {
                    hasGaps = true
                    continue
                }
                val delta = max(0.0, current - past) * fraction
                if (delta > 0.0) totals[uid] = (totals[uid] ?: 0.0) + delta
            }
        }

        val latest = snapshots.lastOrNull()?.at ?: 0L
        if (latest > 0L && now - latest > GAP_THRESHOLD_MS) hasGaps = true
        return UsageSummary(
            byUidMah = totals,
            coverageHours = min((now - cutoff).toDouble() / HOUR_MS, coveredMs.toDouble() / HOUR_MS),
            hasGaps = hasGaps,
            updatedAt = latest,
            hasTwoSamples = observedIntervals > 0,
        )
    }

    fun isReset(previous: Map<Int, Double>, current: Map<Int, Double>): Boolean {
        if (previous.isEmpty()) return false
        val previousTotal = previous.values.sum()
        if (previousTotal < 1.0) return false
        val currentTotal = current.values.sum()
        val dropped = previous.count { (uid, amount) ->
            amount > 0.01 && (current[uid] ?: 0.0) + ROUNDING_TOLERANCE_MAH < amount
        }
        return dropped >= 3 && currentTotal < previousTotal * 0.75
    }
}
