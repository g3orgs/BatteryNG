package de.batteryng.mvp.measurement

import android.os.BatteryManager

enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
    UNKNOWN,
}

enum class BatteryPowerSource {
    BATTERY,
    AC,
    USB,
    WIRELESS,
    DOCK,
    MULTIPLE,
    UNKNOWN,
}

/** Values available to ordinary apps. Missing or unsupported values stay null. */
data class BatteryDetails(
    val levelPercent: Double? = null,
    val temperatureCelsius: Double? = null,
    val voltageVolts: Double? = null,
    val cycleCount: Int? = null,
    val health: BatteryHealth = BatteryHealth.UNKNOWN,
    val technology: String? = null,
    val powerSource: BatteryPowerSource = BatteryPowerSource.UNKNOWN,
    /** The current charge counter, not the design or full-charge capacity. */
    val remainingCapacityMah: Double? = null,
    /** No public BatteryManager property exposes an exact full-charge capacity. */
    val maxCapacityMah: Double? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val buildId: String? = null,
)

internal data class RawBatteryDetails(
    val level: Int? = null,
    val scale: Int? = null,
    val fallbackCapacityPercent: Int? = null,
    val temperatureTenthsCelsius: Int? = null,
    val voltageMillivolts: Int? = null,
    val cycleCount: Int? = null,
    val healthCode: Int? = null,
    val pluggedCode: Int? = null,
    val technology: String? = null,
    val remainingChargeMicroAmpHours: Int? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val buildId: String? = null,
)

/** Unit conversion and validation of data read from ACTION_BATTERY_CHANGED. */
internal object BatteryDetailsResolver {
    fun resolve(raw: RawBatteryDetails): BatteryDetails {
        val level = raw.level
        val scale = raw.scale
        val levelPercent = if (level != null && scale != null && scale > 0 && level in 0..scale) {
            level.toDouble() * 100.0 / scale
        } else {
            raw.fallbackCapacityPercent?.takeIf { it in 0..100 }?.toDouble()
        }
        val temperature = raw.temperatureTenthsCelsius
            ?.takeIf { it in -400..1_000 }
            ?.div(10.0)
        val voltage = raw.voltageMillivolts
            ?.takeIf { it > 0 }
            ?.div(1_000.0)
        val cycles = raw.cycleCount?.takeIf { it > 0 }
        val remainingCapacity = raw.remainingChargeMicroAmpHours
            ?.takeIf { it > 0 }
            ?.div(1_000.0)
        return BatteryDetails(
            levelPercent = levelPercent,
            temperatureCelsius = temperature,
            voltageVolts = voltage,
            cycleCount = cycles,
            health = health(raw.healthCode),
            technology = clean(raw.technology),
            powerSource = source(raw.pluggedCode),
            remainingCapacityMah = remainingCapacity,
            manufacturer = clean(raw.manufacturer),
            model = clean(raw.model),
            androidVersion = clean(raw.androidVersion),
            buildId = clean(raw.buildId),
        )
    }

    private fun health(code: Int?): BatteryHealth = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }

    private fun source(code: Int?): BatteryPowerSource {
        if (code == null || code < 0) return BatteryPowerSource.UNKNOWN
        if (code == 0) return BatteryPowerSource.BATTERY
        val knownMask = BatteryManager.BATTERY_PLUGGED_AC or
            BatteryManager.BATTERY_PLUGGED_USB or
            BatteryManager.BATTERY_PLUGGED_WIRELESS or
            BatteryManager.BATTERY_PLUGGED_DOCK
        if ((code and knownMask) != code) return BatteryPowerSource.UNKNOWN
        if (Integer.bitCount(code) > 1) return BatteryPowerSource.MULTIPLE
        return when (code) {
            BatteryManager.BATTERY_PLUGGED_AC -> BatteryPowerSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> BatteryPowerSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPowerSource.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> BatteryPowerSource.DOCK
            else -> BatteryPowerSource.UNKNOWN
        }
    }

    private fun clean(value: String?): String? = value?.trim()
        ?.takeUnless { it.isBlank() || it.equals("unknown", ignoreCase = true) }
}
