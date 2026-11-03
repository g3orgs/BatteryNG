package de.batteryng.mvp.usage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/** Best-effort power-event hook; the sampler and live overlay also observe battery state. */
class UsagePowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED ||
            intent.action == Intent.ACTION_POWER_DISCONNECTED
        ) {
            val pending = goAsync()
            val observation = runCatching {
                UsageChargeTracker.observe(
                    context,
                    action = intent.action,
                    scheduleBaseline = false,
                )
            }.getOrElse {
                pending.finish()
                return
            }
            if (!observation.newlyQualified) {
                pending.finish()
                return
            }
            val mainHandler = Handler(Looper.getMainLooper())
            val done = AtomicBoolean(false)
            val finish = {
                if (done.compareAndSet(false, true)) pending.finish()
            }
            // A broadcast must finish promptly even if dumpsys stalls.
            mainHandler.postDelayed(finish, 8_000L)
            UsageTopThreeRepository.collectChargeBaseline(
                context,
                observation.lastFullChargeAt,
                finished = finish,
            )
        }
    }
}
