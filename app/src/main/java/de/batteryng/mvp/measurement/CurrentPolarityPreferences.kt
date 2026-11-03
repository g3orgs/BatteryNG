package de.batteryng.mvp.measurement

import android.content.Context

/** How the device's signed battery-current property should be interpreted. */
enum class CurrentPolarityMode {
    AUTOMATIC,
    STANDARD,
    REVERSED,
}

/** One device-wide choice shared by the main screen and the overlay. */
object CurrentPolarityPreferences {
    private const val PREFS = "batteryng_measurement"
    private const val MODE = "current_polarity_mode"
    private const val AUTO_DETECTED = "auto_detected_current_polarity"
    private val calibrator = CurrentPolarityCalibrator()

    fun loadMode(context: Context): CurrentPolarityMode {
        val saved = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODE, null)
        return CurrentPolarityMode.entries.firstOrNull { it.name == saved }
            ?: CurrentPolarityMode.AUTOMATIC
    }

    @Synchronized
    fun saveMode(context: Context, mode: CurrentPolarityMode) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(MODE, mode.name)
            .apply()
        calibrator.reset()
    }

    /** Automatic learning uses only stable unplugged, discharging CURRENT_NOW readings. */
    @Synchronized
    internal fun effectivePolarity(
        context: Context,
        rawNowMicroAmps: Long?,
        chargeStatus: BatteryChargeStatus,
        isPluggedIn: Boolean?,
        timestampElapsedRealtimeMs: Long,
    ): CurrentPolarityMode {
        val mode = loadMode(context)
        if (mode != CurrentPolarityMode.AUTOMATIC) {
            calibrator.reset()
            return mode
        }

        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val learned = calibrator.observe(
            rawNowMicroAmps = rawNowMicroAmps,
            chargeStatus = chargeStatus,
            isPluggedIn = isPluggedIn,
            timestampElapsedRealtimeMs = timestampElapsedRealtimeMs,
        )
        if (learned != null && preferences.getString(AUTO_DETECTED, null) != learned.name) {
            preferences.edit().putString(AUTO_DETECTED, learned.name).apply()
        }
        val saved = preferences.getString(AUTO_DETECTED, null)
        return learned ?: CurrentPolarityMode.entries.firstOrNull {
            it.name == saved && it != CurrentPolarityMode.AUTOMATIC
        } ?: CurrentPolarityMode.STANDARD
    }
}

/** Pure calibration state. A charging status while plugged in is never polarity evidence. */
internal class CurrentPolarityCalibrator {
    private var candidate: CurrentPolarityMode? = null
    private var acceptedSamples = 0
    private var lastAcceptedAtMs: Long? = null

    fun observe(
        rawNowMicroAmps: Long?,
        chargeStatus: BatteryChargeStatus,
        isPluggedIn: Boolean?,
        timestampElapsedRealtimeMs: Long,
    ): CurrentPolarityMode? {
        val validCurrent = rawNowMicroAmps != null &&
            rawNowMicroAmps != Long.MIN_VALUE &&
            (rawNowMicroAmps >= MIN_ABSOLUTE_CURRENT_MICROAMPS ||
                rawNowMicroAmps <= -MIN_ABSOLUTE_CURRENT_MICROAMPS)
        if (isPluggedIn != false || chargeStatus != BatteryChargeStatus.DISCHARGING || !validCurrent) {
            reset()
            return null
        }

        val observed = if (rawNowMicroAmps!! > 0) {
            CurrentPolarityMode.REVERSED
        } else {
            CurrentPolarityMode.STANDARD
        }
        val previousAt = lastAcceptedAtMs
        if (observed != candidate || previousAt == null ||
            timestampElapsedRealtimeMs < previousAt ||
            timestampElapsedRealtimeMs - previousAt > MAX_SAMPLE_GAP_MS
        ) {
            candidate = observed
            acceptedSamples = 1
            lastAcceptedAtMs = timestampElapsedRealtimeMs
        } else if (timestampElapsedRealtimeMs - previousAt >= MIN_SAMPLE_GAP_MS) {
            acceptedSamples = (acceptedSamples + 1).coerceAtMost(REQUIRED_SAMPLES)
            lastAcceptedAtMs = timestampElapsedRealtimeMs
        }
        return observed.takeIf { acceptedSamples >= REQUIRED_SAMPLES }
    }

    fun reset() {
        candidate = null
        acceptedSamples = 0
        lastAcceptedAtMs = null
    }

    private companion object {
        const val MIN_ABSOLUTE_CURRENT_MICROAMPS = 50_000L
        const val MIN_SAMPLE_GAP_MS = 1_000L
        const val MAX_SAMPLE_GAP_MS = 30_000L
        const val REQUIRED_SAMPLES = 3
    }
}
