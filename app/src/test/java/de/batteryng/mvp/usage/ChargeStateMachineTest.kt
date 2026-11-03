package de.batteryng.mvp.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeStateMachineTest {
    private val unknown = ChargeState(false, false, -1.0, false, 0)

    @Test
    fun initialPluggedBatteryAbove96StartsHonestBaselineNow() {
        val transition = ChargeStateMachine.advance(unknown, true, 97.0, null, 1_000)
        assertTrue(transition.newlyQualified)
        assertEquals(1_000, transition.state.lastFullChargeAt)
    }

    @Test
    fun crossingAbove96WhilePluggedQualifiesOnlyOnce() {
        val below = ChargeStateMachine.advance(unknown, true, 95.0, null, 1_000)
        assertFalse(below.newlyQualified)
        val full = ChargeStateMachine.advance(below.state, true, 97.0, null, 2_000)
        assertTrue(full.newlyQualified)
        val stillFull = ChargeStateMachine.advance(full.state, true, 100.0, null, 3_000)
        assertFalse(stillFull.newlyQualified)
        assertEquals(2_000, stillFull.state.lastFullChargeAt)
    }

    @Test
    fun newConnectionAbove96ResetsEvenIfDisconnectWasMissed() {
        val full = ChargeState(true, true, 100.0, true, 2_000)
        val reconnected = ChargeStateMachine.advance(
            full, true, 100.0, "android.intent.action.ACTION_POWER_CONNECTED", 4_000
        )
        assertTrue(reconnected.newlyQualified)
        assertEquals(4_000, reconnected.state.lastFullChargeAt)
    }

    @Test
    fun unplugThenReplugAbove96ResetsWithoutManifestBroadcast() {
        val full = ChargeState(true, true, 100.0, true, 2_000)
        val unplugged = ChargeStateMachine.advance(full, false, 99.0, null, 3_000)
        val replugged = ChargeStateMachine.advance(unplugged.state, true, 99.0, null, 4_000)
        assertFalse(unplugged.newlyQualified)
        assertTrue(replugged.newlyQualified)
    }

    @Test
    fun quickFullChargeThenDisconnectQualifiesEvenWithoutIntermediateSample() {
        val chargingBelowThreshold = ChargeState(true, true, 95.0, false, 1_000)
        val disconnected = ChargeStateMachine.advance(
            chargingBelowThreshold,
            false,
            98.0,
            "android.intent.action.ACTION_POWER_DISCONNECTED",
            2_000,
        )

        assertTrue(disconnected.newlyQualified)
        assertEquals(2_000, disconnected.state.lastFullChargeAt)
        assertFalse(disconnected.state.plugged)
        val repeated = ChargeStateMachine.advance(
            disconnected.state,
            false,
            98.0,
            "android.intent.action.ACTION_POWER_DISCONNECTED",
            3_000,
        )
        assertFalse(repeated.newlyQualified)
        assertEquals(2_000, repeated.state.lastFullChargeAt)

        val observedByOverlay = ChargeStateMachine.advance(
            chargingBelowThreshold, false, 98.0, null, 2_000
        )
        assertTrue(observedByOverlay.newlyQualified)
    }
}
