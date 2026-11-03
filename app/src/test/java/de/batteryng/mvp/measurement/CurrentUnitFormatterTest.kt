package de.batteryng.mvp.measurement

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CurrentUnitFormatterTest {
    @Test
    fun microampReadingIsShownInSelectedUnit() {
        val reading = CurrentResolver.resolve(
            nowMicroAmps = 1_318_000,
            averageMicroAmps = null,
            chargeStatus = BatteryChargeStatus.CHARGING,
            isPluggedIn = true,
            timestampElapsedRealtimeMs = 0,
        )
        val milliAmps = requireNotNull(reading.currentMilliAmps)

        assertEquals("+1318 mA", CurrentUnitFormatter.format(milliAmps, CurrentUnit.MILLIAMPS, Locale.GERMAN))
        assertEquals("+1,318 A", CurrentUnitFormatter.format(milliAmps, CurrentUnit.AMPS, Locale.GERMAN))
    }

    @Test
    fun ampereUsesSelectedLocale() {
        assertEquals("-0.175 A", CurrentUnitFormatter.format(-175.0, CurrentUnit.AMPS, Locale.US))
        assertEquals("-0,175 A", CurrentUnitFormatter.format(-175.0, CurrentUnit.AMPS, Locale.GERMAN))
    }

    @Test
    fun milliampereNeverUsesGroupingSeparator() {
        assertEquals("+12345 mA", CurrentUnitFormatter.format(12_345.0, CurrentUnit.MILLIAMPS, Locale.GERMAN))
        assertEquals("+2,5 mA", CurrentUnitFormatter.format(2.5, CurrentUnit.MILLIAMPS, Locale.GERMAN))
    }

    @Test
    fun valuesRoundedToZeroHaveNoSign() {
        assertEquals("0,000 A", CurrentUnitFormatter.format(-0.1, CurrentUnit.AMPS, Locale.GERMAN))
        assertEquals("0,0 mA", CurrentUnitFormatter.format(-0.01, CurrentUnit.MILLIAMPS, Locale.GERMAN))
    }

    @Test
    fun unavailableValueCarriesSelectedUnit() {
        assertEquals("— mA", CurrentUnitFormatter.unavailable(CurrentUnit.MILLIAMPS))
        assertEquals("— A", CurrentUnitFormatter.unavailable(CurrentUnit.AMPS))
    }
}
