package de.batteryng.mvp.measurement

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryDetailsResolverTest {
    @Test
    fun convertsPixelBatteryBroadcastUnitsWithoutInventingMaximumCapacity() {
        val details = BatteryDetailsResolver.resolve(
            RawBatteryDetails(
                level = 95,
                scale = 100,
                temperatureTenthsCelsius = 265,
                voltageMillivolts = 4_220,
                cycleCount = 742,
                healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
                pluggedCode = 0,
                technology = " Li-ion ",
                remainingChargeMicroAmpHours = 3_600_000,
                manufacturer = "Google",
                model = "Pixel 8",
                androidVersion = "17",
                buildId = "CP3A.261005.005",
            )
        )

        assertEquals(95.0, details.levelPercent!!, 0.0)
        assertEquals(26.5, details.temperatureCelsius!!, 0.0)
        assertEquals(4.22, details.voltageVolts!!, 0.0)
        assertEquals(742, details.cycleCount)
        assertEquals(BatteryHealth.GOOD, details.health)
        assertEquals(BatteryPowerSource.BATTERY, details.powerSource)
        assertEquals("Li-ion", details.technology)
        assertEquals(3_600.0, details.remainingCapacityMah!!, 0.0)
        assertNull(details.maxCapacityMah)
        assertEquals("Google", details.manufacturer)
        assertEquals("Pixel 8", details.model)
        assertEquals("17", details.androidVersion)
        assertEquals("CP3A.261005.005", details.buildId)
    }

    @Test
    fun invalidOrUnsupportedValuesRemainUnknown() {
        val details = BatteryDetailsResolver.resolve(
            RawBatteryDetails(
                level = 101,
                scale = 100,
                fallbackCapacityPercent = 94,
                temperatureTenthsCelsius = -65_536,
                voltageMillivolts = 0,
                cycleCount = 0,
                healthCode = -1,
                pluggedCode = 64,
                technology = "  ",
                remainingChargeMicroAmpHours = Int.MIN_VALUE,
                manufacturer = "unknown",
            )
        )

        assertEquals(94.0, details.levelPercent!!, 0.0)
        assertNull(details.temperatureCelsius)
        assertNull(details.voltageVolts)
        assertNull(details.cycleCount)
        assertEquals(BatteryHealth.UNKNOWN, details.health)
        assertEquals(BatteryPowerSource.UNKNOWN, details.powerSource)
        assertNull(details.technology)
        assertNull(details.remainingCapacityMah)
        assertNull(details.manufacturer)
    }

    @Test
    fun reportsMultiplePowerInputsWithoutGuessingWhichOneIsActive() {
        val details = BatteryDetailsResolver.resolve(
            RawBatteryDetails(
                pluggedCode = BatteryManager.BATTERY_PLUGGED_AC or
                    BatteryManager.BATTERY_PLUGGED_USB,
            )
        )

        assertEquals(BatteryPowerSource.MULTIPLE, details.powerSource)
    }
}
