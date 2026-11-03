package de.batteryng.mvp.ui

/** A visual rating of positive net battery current while external power is connected. */
enum class ChargingMood {
    SAD,
    SLOW,
    OKAY,
    GOOD,
    LAUGHING,
}

object ChargingMoodSelector {
    fun select(currentMilliAmps: Double?, isPluggedIn: Boolean?): ChargingMood? {
        if (isPluggedIn != true || currentMilliAmps == null ||
            !currentMilliAmps.isFinite() || currentMilliAmps <= 0.0
        ) return null

        return when {
            currentMilliAmps < 400.0 -> ChargingMood.SAD
            currentMilliAmps < 1_000.0 -> ChargingMood.SLOW
            currentMilliAmps < 1_500.0 -> ChargingMood.OKAY
            currentMilliAmps < 2_000.0 -> ChargingMood.GOOD
            else -> ChargingMood.LAUGHING
        }
    }
}
