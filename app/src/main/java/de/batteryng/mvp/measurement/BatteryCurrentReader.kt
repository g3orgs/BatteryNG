package de.batteryng.mvp.measurement

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock

/** Reads the signed net battery current from Android; no special permission is required. */
class BatteryCurrentReader(context: Context) {
    private val applicationContext = context.applicationContext
    private val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    fun read(): BatteryCurrentReading {
        val now = readProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val average = if (now == null || now == Long.MIN_VALUE) {
            readProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        } else {
            null
        }
        val batteryIntent = try {
            applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: RuntimeException) {
            null
        }

        val details = BatteryDetailsResolver.resolve(
            RawBatteryDetails(
                level = batteryIntent.optionalInt(BatteryManager.EXTRA_LEVEL),
                scale = batteryIntent.optionalInt(BatteryManager.EXTRA_SCALE),
                fallbackCapacityPercent = readIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                temperatureTenthsCelsius = batteryIntent.optionalInt(BatteryManager.EXTRA_TEMPERATURE),
                voltageMillivolts = batteryIntent.optionalInt(BatteryManager.EXTRA_VOLTAGE),
                cycleCount = if (Build.VERSION.SDK_INT >= 34) {
                    batteryIntent.optionalInt(BatteryManager.EXTRA_CYCLE_COUNT)
                } else null,
                healthCode = batteryIntent.optionalInt(BatteryManager.EXTRA_HEALTH),
                pluggedCode = batteryIntent.optionalInt(BatteryManager.EXTRA_PLUGGED),
                technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
                remainingChargeMicroAmpHours = readIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER
                ),
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                buildId = Build.ID,
            )
        )

        val chargeStatus = batteryIntent.chargeStatus()
        val isPluggedIn = batteryIntent.isPluggedIn()
        val timestamp = SystemClock.elapsedRealtime()
        val polarity = CurrentPolarityPreferences.effectivePolarity(
            context = applicationContext,
            rawNowMicroAmps = now,
            chargeStatus = chargeStatus,
            isPluggedIn = isPluggedIn,
            timestampElapsedRealtimeMs = timestamp,
        )
        return CurrentResolver.resolve(
            nowMicroAmps = now,
            averageMicroAmps = average,
            chargeStatus = chargeStatus,
            isPluggedIn = isPluggedIn,
            timestampElapsedRealtimeMs = timestamp,
            polarity = polarity,
        ).copy(details = details)
    }

    private fun readProperty(property: Int): Long? = try {
        batteryManager?.getLongProperty(property)
    } catch (_: RuntimeException) {
        null
    }

    private fun readIntProperty(property: Int): Int? = try {
        batteryManager?.getIntProperty(property)?.takeUnless { it == Int.MIN_VALUE }
    } catch (_: RuntimeException) {
        null
    }

    private fun Intent?.optionalInt(extra: String): Int? =
        this?.takeIf { it.hasExtra(extra) }
            ?.getIntExtra(extra, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }

    private fun Intent?.chargeStatus(): BatteryChargeStatus = when (
        this?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    ) {
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargeStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
        BatteryManager.BATTERY_STATUS_FULL -> BatteryChargeStatus.FULL
        else -> BatteryChargeStatus.UNKNOWN
    }

    private fun Intent?.isPluggedIn(): Boolean? = when (
        this?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    ) {
        -1 -> null
        0 -> false
        else -> true
    }
}
