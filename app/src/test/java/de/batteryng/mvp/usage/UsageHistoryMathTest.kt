package de.batteryng.mvp.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageHistoryMathTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val appUid = 10_333

    @Test
    fun rollingWindowUsesOnlyOverlappingPartsOfIntervalsAcrossCharging() {
        val now = 30 * hour
        val samples = listOf(
            snapshot(now - 24 * hour - 45 * minute, 1.0),
            snapshot(now - 24 * hour - 15 * minute, 3.0), // Entirely older than 24 h.
            snapshot(now - 24 * hour + 15 * minute, 7.0), // Half of this interval overlaps.
            snapshot(now - 24 * hour + 45 * minute, 13.0),
        )

        val summary = UsageHistoryMath.summarize(samples, now)

        assertEquals(8.0, summary.byUidMah[appUid]!!, 0.000001)
        assertEquals(0.75, summary.coverageHours, 0.000001)
        assertTrue(summary.hasGaps) // No sample after this early interval.
        assertTrue(summary.hasTwoSamples)
    }

    @Test
    fun resetDiscardsOnlyAffectedIntervalThenResumesFromNewBaseline() {
        val now = hour
        val samples = listOf(
            snapshot(30 * minute, 10.0),
            snapshot(45 * minute, 2.0, resetBefore = true),
            snapshot(now, 3.0),
        )

        val summary = UsageHistoryMath.summarize(samples, now)

        assertEquals(1.0, summary.byUidMah[appUid]!!, 0.000001)
        assertEquals(0.25, summary.coverageHours, 0.000001)
        assertTrue(summary.hasGaps)
        assertTrue(summary.hasTwoSamples)
    }

    @Test
    fun longGapDoesNotCreateEstimatedTopAppFromSevenDayDelta() {
        val now = 7L * 24L * hour
        val samples = listOf(snapshot(0, 10.0), snapshot(now, 100.0))

        val summary = UsageHistoryMath.summarize(samples, now)

        assertTrue(summary.byUidMah.isEmpty())
        assertEquals(0.0, summary.coverageHours, 0.000001)
        assertTrue(summary.hasGaps)
        assertTrue(summary.hasTwoSamples)
    }

    @Test
    fun negativeUidChangeIsSkippedWithoutDiscardingOtherApps() {
        val now = 15 * minute
        val summary = UsageHistoryMath.summarize(
            listOf(
                UsageSnapshot(0, 0, mapOf(appUid to 5.0, 10_444 to 1.0)),
                UsageSnapshot(now, now, mapOf(appUid to 4.0, 10_444 to 3.0)),
            ),
            now,
        )

        assertFalse(summary.byUidMah.containsKey(appUid))
        assertEquals(2.0, summary.byUidMah[10_444]!!, 0.000001)
        assertTrue(summary.hasGaps)
    }

    @Test
    fun firstSeenUidGetsBaselineBeforeItsConsumptionIsCounted() {
        val now = 30 * minute
        val summary = UsageHistoryMath.summarize(
            listOf(
                UsageSnapshot(0, 0, emptyMap()),
                snapshot(15 * minute, 5.0),
                snapshot(now, 7.0),
            ),
            now,
        )

        assertEquals(2.0, summary.byUidMah[appUid]!!, 0.000001)
    }

    @Test
    fun detectsGlobalCounterResetButNotOneUidCorrection() {
        val previous = mapOf(10_001 to 4.0, 10_002 to 3.0, 10_003 to 2.0)
        assertTrue(UsageHistoryMath.isReset(previous, mapOf(10_001 to 0.1, 10_002 to 0.2, 10_003 to 0.3)))
        assertFalse(UsageHistoryMath.isReset(previous, mapOf(10_001 to 3.9, 10_002 to 3.1, 10_003 to 2.2)))
    }

    @Test
    fun sinceFullChargeWindowCanExceed24Hours() {
        val quarterHour = 15 * minute
        val snapshots = (0..192).map { index ->
            snapshot(index * quarterHour, index.toDouble() / 4.0)
        }
        val now = 48 * hour

        val sinceCharge = UsageHistoryMath.summarize(snapshots, now, windowStartAt = 0)
        val last24Hours = UsageHistoryMath.summarize(snapshots, now)

        assertEquals(48.0, sinceCharge.byUidMah[appUid]!!, 0.000001)
        assertEquals(48.0, sinceCharge.coverageHours, 0.000001)
        assertEquals(24.0, last24Hours.byUidMah[appUid]!!, 0.000001)
        assertEquals(24.0, last24Hours.coverageHours, 0.000001)
    }

    @Test
    fun chargeBoundaryNeverAttributesEarlierDrainToNewChargeCycle() {
        val marker = 7 * minute + 30_000L
        val samples = listOf(
            snapshot(0, 0.0),
            snapshot(15 * minute, 10.0),
            snapshot(30 * minute, 12.0),
        )

        val sinceCharge = UsageHistoryMath.summarize(
            samples, 30 * minute, marker, strictStartBoundary = true
        )
        val interpolated = UsageHistoryMath.summarize(samples, 30 * minute, marker)

        assertEquals(2.0, sinceCharge.byUidMah[appUid]!!, 0.000001)
        assertEquals(0.25, sinceCharge.coverageHours, 0.000001)
        assertTrue(sinceCharge.hasGaps)
        assertEquals(7.0, interpolated.byUidMah[appUid]!!, 0.000001)
    }

    private fun snapshot(at: Long, mah: Double, resetBefore: Boolean = false) =
        UsageSnapshot(at, at, mapOf(appUid to mah), resetBefore = resetBefore)
}
