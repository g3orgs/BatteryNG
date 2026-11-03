package de.batteryng.mvp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.batteryng.mvp.measurement.BatteryCurrentReader
import de.batteryng.mvp.measurement.BatteryCurrentReading
import de.batteryng.mvp.measurement.BatteryChargeStatus
import de.batteryng.mvp.measurement.BatteryHealth
import de.batteryng.mvp.measurement.BatteryPowerSource
import de.batteryng.mvp.measurement.CurrentDirection
import de.batteryng.mvp.measurement.CurrentUnit
import de.batteryng.mvp.measurement.CurrentUnitFormatter
import de.batteryng.mvp.measurement.CurrentUnitPreferences
import de.batteryng.mvp.overlay.CurrentOverlayService
import de.batteryng.mvp.ui.BatteryLevelView
import de.batteryng.mvp.ui.ChargingMood
import de.batteryng.mvp.ui.ChargingMoodSelector
import de.batteryng.mvp.ui.ChargingMoodView
import de.batteryng.mvp.ui.CurrentFlowBarView
import de.batteryng.mvp.usage.UsagePeriod
import de.batteryng.mvp.usage.UsageChargeTracker
import de.batteryng.mvp.usage.UsagePreferences
import de.batteryng.mvp.usage.UsageState
import de.batteryng.mvp.usage.UsageTopThreeRepository
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** The concise live view: current measurement and app energy ranking. */
class MainActivity : BatteryNgActivity() {
    private lateinit var reader: BatteryCurrentReader
    private lateinit var mainContent: ViewGroup
    private lateinit var currentCard: View
    private lateinit var chargingMood: ChargingMoodView
    private lateinit var currentTemperature: TextView
    private lateinit var currentValue: TextView
    private lateinit var currentDirection: TextView
    private lateinit var batteryLevelIcon: BatteryLevelView
    private lateinit var batteryLevelValue: TextView
    private lateinit var currentMin: TextView
    private lateinit var currentMax: TextView
    private lateinit var currentFlowBar: CurrentFlowBarView
    private lateinit var plugIcon: ImageView
    private lateinit var detailsRows: LinearLayout
    private lateinit var detailsCard: View
    private lateinit var detailsHeader: View
    private lateinit var detailsTitle: TextView
    private lateinit var detailsChevron: ImageView
    private lateinit var detailsBody: View
    private lateinit var topAppsCard: View
    private lateinit var topAppsHeader: View
    private lateinit var topAppsTitle: TextView
    private lateinit var topAppsChevron: ImageView
    private lateinit var topAppsBody: View
    private lateinit var topAppsPeriod: TextView
    private lateinit var topAppsStatus: TextView
    private lateinit var topAppsCoverage: TextView
    private lateinit var topAppsRows: LinearLayout
    private var usageLoadGeneration = 0
    private var lastChargeObservationAt = 0L
    private var lastKnownFullChargeAt = 0L
    private var minObservedMilliAmps: Double? = null
    private var maxObservedMilliAmps: Double? = null
    private var lastPolarityReversed: Boolean? = null
    private var lastCardDirection: CurrentDirection? = null
    private var currentUnit = CurrentUnit.MILLIAMPS
    private var detailsExpanded = true
    private var topAppsExpanded = true
    private val detailValues = mutableMapOf<Int, TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() {
            render(reader.read())
            val now = SystemClock.elapsedRealtime()
            if (now - lastChargeObservationAt >= CHARGE_OBSERVATION_INTERVAL_MS) {
                val marker = UsageChargeTracker.observe(this@MainActivity).lastFullChargeAt
                if (marker != lastKnownFullChargeAt) {
                    lastKnownFullChargeAt = marker
                    if (topAppsCard.visibility == View.VISIBLE &&
                        UsagePreferences.load(this@MainActivity).period == UsagePeriod.SINCE_FULL_CHARGE
                    ) {
                        loadTopApps()
                    }
                }
                lastChargeObservationAt = now
            }
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reader = BatteryCurrentReader(this)
        mainContent = findViewById(R.id.mainContent)
        currentCard = findViewById(R.id.currentCard)
        chargingMood = findViewById(R.id.chargingMood)
        currentTemperature = findViewById(R.id.currentTemperature)
        currentValue = findViewById(R.id.currentValue)
        currentUnit = CurrentUnitPreferences.load(this)
        currentValue.text = CurrentUnitFormatter.unavailable(currentUnit)
        currentDirection = findViewById(R.id.currentDirection)
        batteryLevelIcon = findViewById(R.id.batteryLevelIcon)
        batteryLevelValue = findViewById(R.id.batteryLevelValue)
        currentMin = findViewById(R.id.currentMin)
        currentMax = findViewById(R.id.currentMax)
        currentFlowBar = findViewById(R.id.currentFlowBar)
        plugIcon = findViewById(R.id.plugIcon)
        detailsRows = findViewById(R.id.detailsRows)
        detailsCard = findViewById(R.id.detailsCard)
        detailsHeader = findViewById(R.id.detailsHeader)
        detailsTitle = findViewById(R.id.detailsTitle)
        detailsChevron = findViewById(R.id.detailsChevron)
        detailsBody = findViewById(R.id.detailsBody)
        topAppsCard = findViewById(R.id.topAppsCard)
        topAppsHeader = findViewById(R.id.topAppsHeader)
        topAppsTitle = findViewById(R.id.topAppsTitle)
        topAppsChevron = findViewById(R.id.topAppsChevron)
        topAppsBody = findViewById(R.id.topAppsBody)
        topAppsPeriod = findViewById(R.id.topAppsPeriod)
        topAppsStatus = findViewById(R.id.topAppsStatus)
        topAppsCoverage = findViewById(R.id.topAppsCoverage)
        topAppsRows = findViewById(R.id.topAppsRows)
        findViewById<Button>(R.id.androidBatteryUsageButton).setOnClickListener {
            openAndroidBatteryUsage()
        }
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        createDetailRows()
        detailsExpanded = savedInstanceState?.getBoolean(STATE_DETAILS_EXPANDED) ?: true
        topAppsExpanded = savedInstanceState?.getBoolean(STATE_TOP_APPS_EXPANDED) ?: true
        updateSection(detailsHeader, detailsTitle, detailsChevron, detailsBody, detailsExpanded)
        updateSection(topAppsHeader, topAppsTitle, topAppsChevron, topAppsBody, topAppsExpanded)
        detailsHeader.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainContent)
            detailsExpanded = !detailsExpanded
            updateSection(detailsHeader, detailsTitle, detailsChevron, detailsBody, detailsExpanded)
        }
        topAppsHeader.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainContent)
            topAppsExpanded = !topAppsExpanded
            updateSection(topAppsHeader, topAppsTitle, topAppsChevron, topAppsBody, topAppsExpanded)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_DETAILS_EXPANDED, detailsExpanded)
        outState.putBoolean(STATE_TOP_APPS_EXPANDED, topAppsExpanded)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        CurrentOverlayService.setAppForeground(true)
        lastKnownFullChargeAt = UsageChargeTracker.observe(this).lastFullChargeAt
        lastChargeObservationAt = SystemClock.elapsedRealtime()
        UsageTopThreeRepository.schedule(applicationContext)
        currentUnit = CurrentUnitPreferences.load(this)
        detailsCard.visibility = if (DisplayPreferences.showBatteryDetails(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val showTopApps = UsagePreferences.load(this).showTopApps
        topAppsCard.visibility = if (showTopApps) View.VISIBLE else View.GONE
        if (showTopApps) updateRankingHeader()
        handler.post(refreshTask)
        if (showTopApps) loadTopApps()
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        usageLoadGeneration++
        CurrentOverlayService.setAppForeground(false)
        super.onPause()
    }

    private fun updateRankingHeader() {
        val settings = UsagePreferences.load(this)
        topAppsTitle.text = getString(R.string.top_apps_count_title, settings.topCount)
        updateSection(topAppsHeader, topAppsTitle, topAppsChevron, topAppsBody, topAppsExpanded)
        topAppsPeriod.setText(
            if (settings.period == UsagePeriod.LAST_24_HOURS) R.string.top_apps_period_24h
            else R.string.top_apps_period_charge
        )
    }

    private fun loadTopApps() {
        val generation = ++usageLoadGeneration
        renderTopApps(UsageState.Loading)
        UsageTopThreeRepository.load(applicationContext) { state ->
            runOnUiThread {
                if (generation == usageLoadGeneration && !isDestroyed) renderTopApps(state)
            }
        }
    }

    private fun openAndroidBatteryUsage() {
        try {
            startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.android_battery_usage_unavailable, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.android_battery_usage_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun renderTopApps(state: UsageState) {
        topAppsRows.removeAllViews()
        topAppsRows.visibility = View.GONE
        topAppsCoverage.visibility = View.GONE
        topAppsStatus.visibility = View.VISIBLE
        when (state) {
            UsageState.Loading -> topAppsStatus.setText(R.string.top_apps_loading)
            UsageState.Collecting -> topAppsStatus.setText(R.string.top_apps_collecting)
            UsageState.AwaitingFullCharge -> topAppsStatus.setText(R.string.top_apps_awaiting_charge)
            UsageState.Error -> topAppsStatus.setText(R.string.top_apps_error)
            UsageState.PermissionRequired -> topAppsStatus.setText(R.string.top_apps_permission_short)
            UsageState.Empty -> topAppsStatus.setText(R.string.top_apps_empty)
            is UsageState.Data -> {
                topAppsTitle.text = getString(R.string.top_apps_count_title, state.topCount)
                updateSection(topAppsHeader, topAppsTitle, topAppsChevron, topAppsBody, topAppsExpanded)
                topAppsPeriod.setText(
                    if (state.period == UsagePeriod.LAST_24_HOURS) R.string.top_apps_period_24h
                    else R.string.top_apps_period_charge
                )
                topAppsCoverage.visibility = View.VISIBLE
                topAppsCoverage.text = coverageLabel(state)
                if (state.items.isEmpty()) {
                    topAppsStatus.setText(R.string.top_apps_no_drain)
                    return
                }
                topAppsStatus.visibility = View.GONE
                topAppsRows.visibility = View.VISIBLE
                state.items.take(state.topCount).forEachIndexed { index, item ->
                    val row = LayoutInflater.from(this).inflate(R.layout.top_app_row, topAppsRows, false)
                    row.findViewById<TextView>(R.id.topAppRank).text = (index + 1).toString() + "."
                    row.findViewById<ImageView>(R.id.topAppIcon)
                        .setImageDrawable(item.icon ?: getDrawable(android.R.drawable.sym_def_app_icon))
                    row.findViewById<TextView>(R.id.topAppName).text =
                        item.appName.ifBlank { item.packageName }
                    val amount = item.estimatedMah
                    row.findViewById<TextView>(R.id.topAppMah).text = when {
                        !amount.isFinite() -> "— mAh"
                        amount > 0.0 && amount < 1.0 -> "<1 mAh"
                        else -> String.format(Locale.getDefault(), "%,.0f mAh", amount.coerceAtLeast(0.0))
                    }
                    topAppsRows.addView(row)
                }
            }
        }
    }

    private fun updateSection(
        header: View,
        title: TextView,
        chevron: ImageView,
        body: View,
        expanded: Boolean,
    ) {
        body.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.rotation = if (expanded) 180f else 0f
        header.contentDescription = getString(
            if (expanded) R.string.section_collapse else R.string.section_expand,
            title.text,
        )
    }

    private fun coverageLabel(state: UsageState.Data): String {
        val hours = if (state.coverageHours.isFinite()) state.coverageHours.coerceAtLeast(0.0) else 0.0
        val duration = if (hours < 1.0) {
            getString(R.string.top_apps_duration_minutes, (hours * 60).roundToInt())
        } else {
            getString(R.string.top_apps_duration_hours, String.format(Locale.getDefault(), "%.1f", hours))
        }
        val coverage = if (state.period == UsagePeriod.LAST_24_HOURS) {
            if (hours >= 23.95 && !state.hasGaps) getString(R.string.top_apps_coverage_full)
            else getString(R.string.top_apps_coverage_24h, duration)
        } else {
            val start = if (state.windowStartAt > 0L) {
                val formatted = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault(),
                ).format(Date(state.windowStartAt))
                getString(R.string.top_apps_charge_start, formatted)
            } else {
                getString(R.string.top_apps_period_charge)
            }
            start + " · " + getString(R.string.top_apps_coverage_charge, duration)
        }
        val gapsLabel = if (state.hasGaps) getString(R.string.top_apps_coverage_gaps) else ""
        val updatedLabel = if (state.updatedAt > 0L) {
            val formatted = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault(),
            ).format(Date(state.updatedAt))
            getString(R.string.top_apps_updated_at, formatted)
        } else ""
        return coverage + gapsLabel + updatedLabel
    }

    private fun createDetailRows() {
        val labels = listOf(
            R.string.detail_status,
            R.string.detail_connection,
            R.string.detail_level,
            R.string.detail_health,
            R.string.detail_cycles,
            R.string.detail_technology,
            R.string.detail_max_capacity,
            R.string.detail_remaining_capacity,
            R.string.detail_temperature,
            R.string.detail_voltage,
            R.string.detail_manufacturer,
            R.string.detail_model,
            R.string.detail_android_version,
            R.string.detail_build_id,
        )
        labels.forEach { labelRes ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            val label = TextView(this).apply {
                setText(labelRes)
                setTextColor(getColor(R.color.secondary))
                textSize = 14f
            }
            val value = TextView(this).apply {
                setText(R.string.detail_unknown)
                setTextColor(getColor(R.color.primary))
                textSize = 14f
                gravity = Gravity.END
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            row.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(value, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            detailsRows.addView(row)
            detailValues[labelRes] = value
        }
    }

    private fun setDetail(labelRes: Int, text: String?) {
        detailValues[labelRes]?.text = text?.takeIf { it.isNotBlank() }
            ?: getString(R.string.detail_unknown)
    }

    private fun renderDetails(reading: BatteryCurrentReading) {
        val details = reading.details
        val locale = Locale.getDefault()
        val temperature = details.temperatureCelsius?.let {
            String.format(locale, "%.1f °C", it)
        }
        currentTemperature.text = temperature ?: getString(R.string.detail_unknown)
        setDetail(R.string.detail_status, getString(when (reading.chargeStatus) {
            BatteryChargeStatus.CHARGING -> R.string.detail_status_charging
            BatteryChargeStatus.DISCHARGING -> R.string.detail_status_discharging
            BatteryChargeStatus.NOT_CHARGING -> R.string.detail_status_not_charging
            BatteryChargeStatus.FULL -> R.string.detail_status_full
            BatteryChargeStatus.UNKNOWN -> R.string.detail_status_unknown
        }))
        setDetail(R.string.detail_connection, getString(when (details.powerSource) {
            BatteryPowerSource.BATTERY -> R.string.detail_power_battery
            BatteryPowerSource.AC -> R.string.detail_power_ac
            BatteryPowerSource.USB -> R.string.detail_power_usb
            BatteryPowerSource.WIRELESS -> R.string.detail_power_wireless
            BatteryPowerSource.DOCK -> R.string.detail_power_dock
            BatteryPowerSource.MULTIPLE -> R.string.detail_power_multiple
            BatteryPowerSource.UNKNOWN -> R.string.detail_unknown
        }))
        setDetail(R.string.detail_level, details.levelPercent?.let {
            String.format(locale, "%.0f %%", it)
        })
        setDetail(R.string.detail_health, getString(when (details.health) {
            BatteryHealth.GOOD -> R.string.detail_health_good
            BatteryHealth.OVERHEAT -> R.string.detail_health_overheat
            BatteryHealth.DEAD -> R.string.detail_health_dead
            BatteryHealth.OVER_VOLTAGE -> R.string.detail_health_overvoltage
            BatteryHealth.UNSPECIFIED_FAILURE -> R.string.detail_health_unspecified_failure
            BatteryHealth.COLD -> R.string.detail_health_cold
            BatteryHealth.UNKNOWN -> R.string.detail_health_unknown
        }))
        setDetail(R.string.detail_cycles, details.cycleCount?.toString())
        setDetail(R.string.detail_technology, details.technology)
        setDetail(R.string.detail_max_capacity, details.maxCapacityMah?.let {
            String.format(locale, "%,.0f mAh", it)
        })
        setDetail(R.string.detail_remaining_capacity, details.remainingCapacityMah?.let {
            String.format(locale, "%,.0f mAh", it)
        })
        setDetail(R.string.detail_temperature, temperature)
        setDetail(R.string.detail_voltage, details.voltageVolts?.let {
            String.format(locale, "%.2f V", it)
        })
        setDetail(R.string.detail_manufacturer, details.manufacturer)
        setDetail(R.string.detail_model, details.model)
        setDetail(R.string.detail_android_version, details.androidVersion)
        setDetail(R.string.detail_build_id, details.buildId)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun render(reading: BatteryCurrentReading) {
        if (lastPolarityReversed != null && lastPolarityReversed != reading.polarityReversed) {
            minObservedMilliAmps = null
            maxObservedMilliAmps = null
        }
        lastPolarityReversed = reading.polarityReversed
        val milliAmps = reading.currentMilliAmps
        val mood = ChargingMoodSelector.select(milliAmps, reading.isPluggedIn)
        chargingMood.setMood(mood)
        chargingMood.visibility = if (mood == null) View.GONE else View.VISIBLE
        chargingMood.contentDescription = mood?.let { getString(when (it) {
            ChargingMood.SAD -> R.string.charging_mood_sad
            ChargingMood.SLOW -> R.string.charging_mood_slow
            ChargingMood.OKAY -> R.string.charging_mood_okay
            ChargingMood.GOOD -> R.string.charging_mood_good
            ChargingMood.LAUGHING -> R.string.charging_mood_laughing
        }) }
        currentValue.text = milliAmps?.let { CurrentUnitFormatter.format(it, currentUnit) }
            ?: CurrentUnitFormatter.unavailable(currentUnit)
        val (directionText, directionColor) = when (reading.direction) {
            CurrentDirection.CHARGING -> R.string.state_charging to R.color.charging
            CurrentDirection.DISCHARGING -> R.string.state_discharging to R.color.discharging
            CurrentDirection.IDLE -> R.string.state_idle to R.color.neutral
            CurrentDirection.UNKNOWN -> R.string.state_unavailable to R.color.neutral
        }
        currentDirection.setText(directionText)
        currentDirection.setTextColor(getColor(directionColor))
        currentValue.setTextColor(getColor(directionColor))
        batteryLevelIcon.setLevelPercent(reading.details.levelPercent)
        batteryLevelIcon.setTintColor(getColor(directionColor))
        batteryLevelValue.text = reading.details.levelPercent?.let {
            String.format(Locale.getDefault(), "%.0f %%", it)
        } ?: getString(R.string.detail_unknown)
        batteryLevelValue.setTextColor(getColor(directionColor))
        plugIcon.imageTintList = ColorStateList.valueOf(getColor(directionColor))
        plugIcon.visibility = if (reading.isPluggedIn == true) View.VISIBLE else View.GONE
        currentFlowBar.setCurrentMilliAmps(milliAmps)
        milliAmps?.takeIf { it.isFinite() }?.let { current ->
            minObservedMilliAmps = minObservedMilliAmps?.let { minOf(it, current) } ?: current
            maxObservedMilliAmps = maxObservedMilliAmps?.let { maxOf(it, current) } ?: current
        }
        currentMin.text = getString(
            R.string.current_min,
            minObservedMilliAmps?.let { CurrentUnitFormatter.format(it, currentUnit) }
                ?: CurrentUnitFormatter.unavailable(currentUnit),
        )
        currentMax.text = getString(
            R.string.current_max,
            maxObservedMilliAmps?.let { CurrentUnitFormatter.format(it, currentUnit) }
                ?: CurrentUnitFormatter.unavailable(currentUnit),
        )
        if (lastCardDirection != reading.direction) {
            val surfaceColor = when (reading.direction) {
                CurrentDirection.CHARGING -> R.color.charging_surface
                CurrentDirection.DISCHARGING -> R.color.discharging_surface
                CurrentDirection.IDLE, CurrentDirection.UNKNOWN -> R.color.neutral_surface
            }
            currentCard.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(getColor(surfaceColor))
                setStroke(dp(1), getColor(R.color.border))
            }
            lastCardDirection = reading.direction
        }
        renderDetails(reading)
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 1_000L
        private const val CHARGE_OBSERVATION_INTERVAL_MS = 5_000L
        private const val STATE_DETAILS_EXPANDED = "details_expanded"
        private const val STATE_TOP_APPS_EXPANDED = "top_apps_expanded"
    }
}
