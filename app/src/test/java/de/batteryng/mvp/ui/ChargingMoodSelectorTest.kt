package de.batteryng.mvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingMoodSelectorTest {
    @Test
    fun mapsThresholdsInMilliamperes() {
        assertEquals(ChargingMood.SAD, ChargingMoodSelector.select(399.9, true))
        assertEquals(ChargingMood.SLOW, ChargingMoodSelector.select(400.0, true))
        assertEquals(ChargingMood.SLOW, ChargingMoodSelector.select(999.9, true))
        assertEquals(ChargingMood.OKAY, ChargingMoodSelector.select(1_000.0, true))
        assertEquals(ChargingMood.GOOD, ChargingMoodSelector.select(1_500.0, true))
        assertEquals(ChargingMood.LAUGHING, ChargingMoodSelector.select(2_000.0, true))
    }

    @Test
    fun hiddenWithoutPositiveCurrentAndExternalPower() {
        assertNull(ChargingMoodSelector.select(null, true))
        assertNull(ChargingMoodSelector.select(0.0, true))
        assertNull(ChargingMoodSelector.select(-100.0, true))
        assertNull(ChargingMoodSelector.select(2_000.0, false))
        assertNull(ChargingMoodSelector.select(2_000.0, null))
        assertNull(ChargingMoodSelector.select(Double.NaN, true))
    }
}
