package de.batteryng.mvp

import android.content.Context

/** Visibility choices for optional home-screen sections. */
object DisplayPreferences {
    private const val PREFS = "batteryng_display"
    private const val SHOW_BATTERY_DETAILS = "show_battery_details"

    fun showBatteryDetails(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(SHOW_BATTERY_DETAILS, true)

    fun setShowBatteryDetails(context: Context, visible: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_BATTERY_DETAILS, visible)
            .apply()
    }
}
