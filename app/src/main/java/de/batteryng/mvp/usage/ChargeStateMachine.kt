package de.batteryng.mvp.usage

internal data class ChargeState(
    val known: Boolean,
    val plugged: Boolean,
    val percent: Double,
    val triggeredInConnection: Boolean,
    val lastFullChargeAt: Long,
)

internal data class ChargeTransition(val state: ChargeState, val newlyQualified: Boolean)

/** One qualification for each observed connection that reaches more than 96 percent. */
internal object ChargeStateMachine {
    private const val FULL_THRESHOLD = 96.0

    fun advance(
        previous: ChargeState,
        plugged: Boolean,
        percent: Double,
        powerAction: String?,
        now: Long,
    ): ChargeTransition {
        val explicitConnection = powerAction == "android.intent.action.ACTION_POWER_CONNECTED"
        val newConnection = explicitConnection || previous.known && !previous.plugged && plugged
        val crossedThreshold = previous.known && previous.plugged &&
            previous.percent in 0.0..FULL_THRESHOLD && percent > FULL_THRESHOLD
        val initialFullBattery = !previous.known && previous.lastFullChargeAt == 0L &&
            plugged && percent > FULL_THRESHOLD
        val triggered = plugged && !explicitConnection && previous.plugged &&
            previous.triggeredInConnection
        val observedDisconnect = powerAction == "android.intent.action.ACTION_POWER_DISCONNECTED" ||
            previous.known && previous.plugged && !plugged
        val reachedFullBeforeDisconnect = observedDisconnect && percent > FULL_THRESHOLD &&
            !previous.triggeredInConnection && (previous.plugged || !previous.known)
        val qualified = (plugged && percent > FULL_THRESHOLD && !triggered &&
            (newConnection || crossedThreshold || initialFullBattery)) || reachedFullBeforeDisconnect
        return ChargeTransition(
            state = ChargeState(
                known = true,
                plugged = plugged,
                percent = percent,
                triggeredInConnection = triggered || qualified,
                lastFullChargeAt = if (qualified) now else previous.lastFullChargeAt,
            ),
            newlyQualified = qualified,
        )
    }
}
