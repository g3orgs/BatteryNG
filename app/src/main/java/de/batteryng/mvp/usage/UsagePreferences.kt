package de.batteryng.mvp.usage

import android.content.Context

enum class UsagePeriod {
    LAST_24_HOURS,
    SINCE_FULL_CHARGE,
}

data class UsageSettings(
    val topCount: Int = 3,
    val period: UsagePeriod = UsagePeriod.LAST_24_HOURS,
    /** Keep collecting history while the ranking card is hidden from the home screen. */
    val showTopApps: Boolean = true,
)

/** Display preferences for the app-consumption list. Sampling is independent of the selected view. */
object UsagePreferences {
    const val MIN_TOP_COUNT = 1
    const val MAX_TOP_COUNT = 10
    private const val PREFS = "batteryng_usage_settings"
    private const val TOP_COUNT = "top_count"
    private const val PERIOD = "period"
    private const val SHOW_TOP_APPS = "show_top_apps"

    fun load(context: Context): UsageSettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val topCount = prefs.getInt(TOP_COUNT, 3).coerceIn(MIN_TOP_COUNT, MAX_TOP_COUNT)
        val period = runCatching {
            UsagePeriod.valueOf(prefs.getString(PERIOD, null) ?: UsagePeriod.LAST_24_HOURS.name)
        }.getOrDefault(UsagePeriod.LAST_24_HOURS)
        return UsageSettings(topCount, period, prefs.getBoolean(SHOW_TOP_APPS, true))
    }

    fun save(context: Context, settings: UsageSettings) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(TOP_COUNT, settings.topCount.coerceIn(MIN_TOP_COUNT, MAX_TOP_COUNT))
            .putString(PERIOD, settings.period.name)
            .putBoolean(SHOW_TOP_APPS, settings.showTopApps)
            .apply()
    }
}
