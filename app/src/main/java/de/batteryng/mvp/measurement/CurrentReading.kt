package de.batteryng.mvp.measurement

/** The Android property used for this reading. */
enum class CurrentSource {
    CURRENT_NOW,
    CURRENT_AVERAGE,
    UNAVAILABLE,
}

/** Direction of the measured net battery current, independent of the charging status. */
enum class CurrentDirection {
    CHARGING,
    DISCHARGING,
    IDLE,
    UNKNOWN,
}

enum class BatteryChargeStatus {
    CHARGING,
    DISCHARGING,
    NOT_CHARGING,
    FULL,
    UNKNOWN,
}

enum class ReadingQuality {
    VALID,
    UNSUPPORTED,
    SIGN_STATUS_MISMATCH,
}

data class BatteryCurrentReading(
    val timestampElapsedRealtimeMs: Long,
    /** Signed net battery current in microamperes. Positive means charging. */
    val currentMicroAmps: Long?,
    val source: CurrentSource,
    val direction: CurrentDirection,
    val chargeStatus: BatteryChargeStatus,
    /** Whether Android reports an external power connection; null means unknown. */
    val isPluggedIn: Boolean?,
    val quality: ReadingQuality,
    /** True when the device's raw current sign was inverted for this reading. */
    val polarityReversed: Boolean = false,
    val details: BatteryDetails = BatteryDetails(),
) {
    /** Signed net battery current in milliamperes, without premature rounding. */
    val currentMilliAmps: Double?
        get() = currentMicroAmps?.toDouble()?.div(1_000.0)
}

/** Pure selection and interpretation logic, separate from Android platform reads. */
object CurrentResolver {
    fun resolve(
        nowMicroAmps: Long?,
        averageMicroAmps: Long?,
        chargeStatus: BatteryChargeStatus,
        isPluggedIn: Boolean?,
        timestampElapsedRealtimeMs: Long,
        polarity: CurrentPolarityMode = CurrentPolarityMode.STANDARD,
    ): BatteryCurrentReading {
        val nowAvailable = nowMicroAmps != null && nowMicroAmps != Long.MIN_VALUE
        val averageAvailable = averageMicroAmps != null && averageMicroAmps != Long.MIN_VALUE
        val source = when {
            nowAvailable -> CurrentSource.CURRENT_NOW
            averageAvailable -> CurrentSource.CURRENT_AVERAGE
            else -> CurrentSource.UNAVAILABLE
        }
        val rawCurrent = when (source) {
            CurrentSource.CURRENT_NOW -> nowMicroAmps
            CurrentSource.CURRENT_AVERAGE -> averageMicroAmps
            CurrentSource.UNAVAILABLE -> null
        }
        // Unsupported MIN_VALUE values were excluded above before any negation.
        val current = if (polarity == CurrentPolarityMode.REVERSED) {
            rawCurrent?.let { -it }
        } else {
            rawCurrent
        }
        val direction = when {
            current == null -> CurrentDirection.UNKNOWN
            current > 0 -> CurrentDirection.CHARGING
            current < 0 -> CurrentDirection.DISCHARGING
            else -> CurrentDirection.IDLE
        }
        val statusConflict =
            (chargeStatus == BatteryChargeStatus.CHARGING && direction == CurrentDirection.DISCHARGING) ||
                (chargeStatus == BatteryChargeStatus.DISCHARGING && direction == CurrentDirection.CHARGING)
        val quality = when {
            current == null -> ReadingQuality.UNSUPPORTED
            statusConflict -> ReadingQuality.SIGN_STATUS_MISMATCH
            else -> ReadingQuality.VALID
        }

        return BatteryCurrentReading(
            timestampElapsedRealtimeMs = timestampElapsedRealtimeMs,
            currentMicroAmps = current,
            source = source,
            direction = direction,
            chargeStatus = chargeStatus,
            isPluggedIn = isPluggedIn,
            quality = quality,
            polarityReversed = polarity == CurrentPolarityMode.REVERSED,
        )
    }
}
