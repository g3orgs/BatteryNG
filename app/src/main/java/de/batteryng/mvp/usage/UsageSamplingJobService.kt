package de.batteryng.mvp.usage

import android.app.job.JobParameters
import android.app.job.JobService
import java.util.concurrent.atomic.AtomicBoolean

/** Periodic sampling while BatteryNG is not open. Android may defer jobs during Doze. */
class UsageSamplingJobService : JobService() {
    private var runningParams: JobParameters? = null
    private var stopped = AtomicBoolean(false)

    override fun onStartJob(params: JobParameters): Boolean {
        runningParams = params
        val cancellation = AtomicBoolean(false)
        stopped = cancellation
        UsageTopThreeRepository.collectFromJob(this, { !cancellation.get() }) {
            // onStopJob may have rescheduled this job while the collector was finishing.
            if (runningParams === params) {
                runningParams = null
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        if (runningParams === params) {
            runningParams = null
            stopped.set(true)
        }
        return true
    }
}
