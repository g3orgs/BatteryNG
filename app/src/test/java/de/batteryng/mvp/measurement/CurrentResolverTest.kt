package de.batteryng.mvp.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentResolverTest {
    @Test
    fun zeroIsValidAndCurrentNowTakesPriority() {
        val reading = resolve(now = 0, average = 280_000)

        assertEquals(CurrentSource.CURRENT_NOW, reading.source)
        assertEquals(0L, reading.currentMicroAmps)
        assertEquals(0.0, reading.currentMilliAmps!!, 0.0)
        assertEquals(CurrentDirection.IDLE, reading.direction)
        assertEquals(ReadingQuality.VALID, reading.quality)
    }

    @Test
    fun positiveNetCurrentIsChargingAndConvertedToMilliamperes() {
        val reading = resolve(now = 625_250, average = null)

        assertEquals(CurrentDirection.CHARGING, reading.direction)
        assertEquals(625.25, reading.currentMilliAmps!!, 0.0)
        assertEquals(ReadingQuality.VALID, reading.quality)
    }

    @Test
    fun unsupportedInstantValueFallsBackToAverage() {
        val reading = resolve(now = Long.MIN_VALUE, average = -735_500)

        assertEquals(CurrentSource.CURRENT_AVERAGE, reading.source)
        assertEquals(-735.5, reading.currentMilliAmps!!, 0.0)
        assertEquals(CurrentDirection.DISCHARGING, reading.direction)
    }

    @Test
    fun twoUnsupportedPropertiesDoNotBecomeZero() {
        val reading = resolve(now = Long.MIN_VALUE, average = Long.MIN_VALUE)

        assertNull(reading.currentMicroAmps)
        assertNull(reading.currentMilliAmps)
        assertEquals(CurrentSource.UNAVAILABLE, reading.source)
        assertEquals(CurrentDirection.UNKNOWN, reading.direction)
        assertEquals(ReadingQuality.UNSUPPORTED, reading.quality)
    }

    @Test
    fun negativeNetCurrentRemainsDischargingWithCableConnected() {
        val reading = CurrentResolver.resolve(
            nowMicroAmps = -125_000,
            averageMicroAmps = null,
            chargeStatus = BatteryChargeStatus.CHARGING,
            isPluggedIn = true,
            timestampElapsedRealtimeMs = 123L,
        )

        assertEquals(CurrentDirection.DISCHARGING, reading.direction)
        assertEquals(-125.0, reading.currentMilliAmps!!, 0.0)
        assertEquals(ReadingQuality.SIGN_STATUS_MISMATCH, reading.quality)
    }

    @Test
    fun reversedDeviceCurrentIsNormalizedBeforeDirectionAndQuality() {
        val charging = CurrentResolver.resolve(
            nowMicroAmps = -1_250_000,
            averageMicroAmps = null,
            chargeStatus = BatteryChargeStatus.CHARGING,
            isPluggedIn = true,
            timestampElapsedRealtimeMs = 123L,
            polarity = CurrentPolarityMode.REVERSED,
        )
        assertEquals(1_250_000L, charging.currentMicroAmps)
        assertEquals(1_250.0, charging.currentMilliAmps!!, 0.0)
        assertEquals(CurrentDirection.CHARGING, charging.direction)
        assertEquals(ReadingQuality.VALID, charging.quality)
        assertEquals(true, charging.polarityReversed)

        val discharging = CurrentResolver.resolve(
            nowMicroAmps = 400_000,
            averageMicroAmps = null,
            chargeStatus = BatteryChargeStatus.DISCHARGING,
            isPluggedIn = false,
            timestampElapsedRealtimeMs = 124L,
            polarity = CurrentPolarityMode.REVERSED,
        )
        assertEquals(-400_000L, discharging.currentMicroAmps)
        assertEquals(CurrentDirection.DISCHARGING, discharging.direction)
        assertEquals(ReadingQuality.VALID, discharging.quality)
        assertEquals(true, discharging.polarityReversed)
    }

    @Test
    fun reversalPreservesZeroAndSkipsUnsupportedSentinel() {
        val zero = CurrentResolver.resolve(
            nowMicroAmps = 0,
            averageMicroAmps = 200_000,
            chargeStatus = BatteryChargeStatus.UNKNOWN,
            isPluggedIn = null,
            timestampElapsedRealtimeMs = 123L,
            polarity = CurrentPolarityMode.REVERSED,
        )
        assertEquals(0L, zero.currentMicroAmps)
        assertEquals(CurrentDirection.IDLE, zero.direction)

        val fallback = CurrentResolver.resolve(
            nowMicroAmps = Long.MIN_VALUE,
            averageMicroAmps = -250_000,
            chargeStatus = BatteryChargeStatus.CHARGING,
            isPluggedIn = true,
            timestampElapsedRealtimeMs = 124L,
            polarity = CurrentPolarityMode.REVERSED,
        )
        assertEquals(CurrentSource.CURRENT_AVERAGE, fallback.source)
        assertEquals(250_000L, fallback.currentMicroAmps)

        val unavailable = CurrentResolver.resolve(
            nowMicroAmps = Long.MIN_VALUE,
            averageMicroAmps = Long.MIN_VALUE,
            chargeStatus = BatteryChargeStatus.UNKNOWN,
            isPluggedIn = null,
            timestampElapsedRealtimeMs = 125L,
            polarity = CurrentPolarityMode.REVERSED,
        )
        assertNull(unavailable.currentMicroAmps)
        assertEquals(ReadingQuality.UNSUPPORTED, unavailable.quality)
    }

    private fun resolve(now: Long?, average: Long?): BatteryCurrentReading = CurrentResolver.resolve(
        nowMicroAmps = now,
        averageMicroAmps = average,
        chargeStatus = BatteryChargeStatus.UNKNOWN,
        isPluggedIn = null,
        timestampElapsedRealtimeMs = 123L,
    )
}
