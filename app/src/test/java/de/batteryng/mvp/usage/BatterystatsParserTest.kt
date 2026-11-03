package de.batteryng.mvp.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class BatterystatsParserTest {
    @Test
    fun parsesPixel8AccumulatedUsageWithoutCountingSubcomponentsOrCharging() {
        val output = """
            Estimated power use (mAh):
              Capacity: 4164, Computed drain: 16.0, actual drain: 16.0
            Global
              cpu: 21.5 apps: 22.7 duration: 27m 54s 551ms
            UID u0a333: 20.5 fg: 17.5 (7m 26s 142ms)
              cpu=4.68 cpu:fg=4.21 audio=7.95 audio:bg=0.709
              (on battery, screen on) cpu=4.23 cpu:fg=4.21 audio=6.42 audio:fg=6.37
              (on battery, screen off/doze) cpu=0.334 cpu:fgs=0.334
              (not on battery, screen on) cpu=0.117 audio=0.709
            UID u10a150: 0.263 bg: 0.263
              cpu=0.115 mobile_radio=0.148
              (on battery, screen on) cpu=0.0776 custom2=1e-3 cpu:bg=0.0776
              (on battery, screen off/doze) cpu=0.0185
              (not on battery, screen on) mobile_radio=0.148
            UID 1000: 9.21 bg: 9.21
              (on battery, screen on) cpu=2.93
        """.trimIndent()

        val result = BatterystatsParser.parse(StringReader(output))

        assertTrue(result.foundUsage)
        assertFalse(result.permissionDenied)
        assertEquals(10.984, result.byUidMah[10333]!!, 0.000001)
        assertEquals(0.0971, result.byUidMah[1_010_150]!!, 0.000001)
        assertFalse(result.byUidMah.containsKey(1000))
    }

    @Test
    fun identifiesPermissionDenialInsteadOfTreatingItAsEmptyUsage() {
        val result = BatterystatsParser.parse(
            StringReader("Permission Denial: can't dump BatteryStats without permission DUMP")
        )

        assertTrue(result.permissionDenied)
        assertFalse(result.foundUsage)
    }
}
