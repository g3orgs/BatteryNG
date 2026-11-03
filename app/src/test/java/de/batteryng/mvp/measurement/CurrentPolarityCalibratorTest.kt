package de.batteryng.mvp.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentPolarityCalibratorTest {
    @Test
    fun learnsReversedOnlyAfterThreeSeparatedUnpluggedSamples() {
        val calibrator = CurrentPolarityCalibrator()
        assertNull(observe(calibrator, 120_000, 0))
        assertNull(observe(calibrator, 120_000, 500))
        assertNull(observe(calibrator, 120_000, 1_000))
        assertEquals(CurrentPolarityMode.REVERSED, observe(calibrator, 120_000, 2_000))
    }

    @Test
    fun learnsStandardAndCanRelearnOppositeAfterFirmwareChange() {
        val calibrator = CurrentPolarityCalibrator()
        assertNull(observe(calibrator, -100_000, 0))
        assertNull(observe(calibrator, -100_000, 1_000))
        assertEquals(CurrentPolarityMode.STANDARD, observe(calibrator, -100_000, 2_000))
        assertNull(observe(calibrator, 100_000, 3_000))
        assertNull(observe(calibrator, 100_000, 4_000))
        assertEquals(CurrentPolarityMode.REVERSED, observe(calibrator, 100_000, 5_000))
    }

    @Test
    fun pluggedChargingAndWeakOrUnavailableReadingsCannotCalibrate() {
        val calibrator = CurrentPolarityCalibrator()
        assertNull(observe(calibrator, 100_000, 0))
        assertNull(observe(calibrator, 100_000, 1_000))
        assertNull(calibrator.observe(-200_000, BatteryChargeStatus.CHARGING, true, 2_000))
        assertNull(observe(calibrator, 100_000, 3_000))
        assertNull(observe(calibrator, 40_000, 4_000))
        assertNull(observe(calibrator, Long.MIN_VALUE, 5_000))
        assertNull(observe(calibrator, 0, 6_000))
        assertNull(observe(calibrator, 100_000, 7_000))
        assertNull(observe(calibrator, 100_000, 8_000))
        assertEquals(CurrentPolarityMode.REVERSED, observe(calibrator, 100_000, 9_000))
    }

    @Test
    fun statusMustAgreeAndLargeTimeGapsRestartEvidence() {
        val calibrator = CurrentPolarityCalibrator()
        assertNull(observe(calibrator, 100_000, 0))
        assertNull(calibrator.observe(100_000, BatteryChargeStatus.UNKNOWN, false, 1_000))
        assertNull(observe(calibrator, 100_000, 2_000))
        assertNull(observe(calibrator, 100_000, 33_001))
        assertNull(observe(calibrator, 100_000, 34_001))
        assertEquals(CurrentPolarityMode.REVERSED, observe(calibrator, 100_000, 35_001))
    }

    private fun observe(
        calibrator: CurrentPolarityCalibrator,
        rawMicroAmps: Long,
        timestampMs: Long,
    ): CurrentPolarityMode? = calibrator.observe(
        rawNowMicroAmps = rawMicroAmps,
        chargeStatus = BatteryChargeStatus.DISCHARGING,
        isPluggedIn = false,
        timestampElapsedRealtimeMs = timestampMs,
    )
}
