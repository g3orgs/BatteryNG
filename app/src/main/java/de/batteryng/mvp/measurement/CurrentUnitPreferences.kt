package de.batteryng.mvp.measurement

import android.content.Context

/** The selected display unit is shared by the main screen and the floating overlay. */
object CurrentUnitPreferences {
    private const val PREFS = "batteryng_display"
    private const val CURRENT_UNIT = "current_unit"

    fun load(context: Context): CurrentUnit {
        val saved = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CURRENT_UNIT, null)
        return CurrentUnit.entries.firstOrNull { it.name == saved } ?: CurrentUnit.MILLIAMPS
    }

    fun save(context: Context, unit: CurrentUnit) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CURRENT_UNIT, unit.name)
            .apply()
    }
}
