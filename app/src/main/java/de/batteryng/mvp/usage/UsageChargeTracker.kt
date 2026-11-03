package de.batteryng.mvp.usage

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

/** Records the beginning of each charging session that reaches more than 96 percent. */
object UsageChargeTracker {
    private const val PREFS = "batteryng_usage_charge"
    private const val LAST_FULL_CHARGE_AT = "last_full_charge_at"
    private const val PLUGGED = "last_plugged"
    private const val PERCENT = "last_percent"
    private const val TRIGGERED = "triggered_this_connection"

    data class Observation(val lastFullChargeAt: Long, val newlyQualified: Boolean)

    fun lastFullChargeAt(context: Context): Long =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(LAST_FULL_CHARGE_AT, 0L)

    @Synchronized
    fun observe(
        context: Context,
        action: String? = null,
        now: Long = System.currentTimeMillis(),
        scheduleBaseline: Boolean = true,
    ): Observation {
        val appContext = context.applicationContext
        val battery = runCatching {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldMarker = prefs.getLong(LAST_FULL_CHARGE_AT, 0L)
        val oldKnown = prefs.contains(PLUGGED)
        val oldPlugged = prefs.getBoolean(PLUGGED, false)
        val oldPercent = prefs.getFloat(PERCENT, -1f).toDouble()
        val wasTriggered = prefs.getBoolean(TRIGGERED, false)
        if (battery == null && action == null) return Observation(oldMarker, false)

        val plugged = when (action) {
            Intent.ACTION_POWER_CONNECTED -> true
            Intent.ACTION_POWER_DISCONNECTED -> false
            else -> (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        }
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) 100.0 * level / scale else oldPercent
        val transition = ChargeStateMachine.advance(
            previous = ChargeState(oldKnown, oldPlugged, oldPercent, wasTriggered, oldMarker),
            plugged = plugged,
            percent = percent,
            powerAction = action,
            now = now,
        )
        val marker = transition.state.lastFullChargeAt
        val qualifies = transition.newlyQualified

        if (!oldKnown || oldPlugged != plugged || abs(oldPercent - percent) > 0.001 ||
            wasTriggered != transition.state.triggeredInConnection || qualifies
        ) {
            prefs.edit()
                .putBoolean(PLUGGED, plugged)
                .putFloat(PERCENT, percent.toFloat())
                .putBoolean(TRIGGERED, transition.state.triggeredInConnection)
                .putLong(LAST_FULL_CHARGE_AT, marker)
                .commit()
        }
        if (qualifies && scheduleBaseline) {
            UsageTopThreeRepository.collectChargeBaseline(appContext, marker)
        }
        return Observation(marker, qualifies)
    }
}
