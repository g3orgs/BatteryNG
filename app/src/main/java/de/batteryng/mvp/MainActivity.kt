package de.batteryng.mvp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.app.job.JobScheduler
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import java.util.Locale
import kotlin.math.roundToInt

/** The concise live battery-current view. */
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
    private var minObservedMilliAmps: Double? = null
    private var maxObservedMilliAmps: Double? = null
    private var lastPolarityReversed: Boolean? = null
    private var lastCardDirection: CurrentDirection? = null
    private var currentUnit = CurrentUnit.MILLIAMPS
    private var detailsExpanded = true
    private val detailValues = mutableMapOf<Int, TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() {
            render(reader.read())
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(LEGACY_USAGE_JOB_ID)
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
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        createDetailRows()
        detailsExpanded = savedInstanceState?.getBoolean(STATE_DETAILS_EXPANDED) ?: true
        updateSection(detailsHeader, detailsTitle, detailsChevron, detailsBody, detailsExpanded)
        detailsHeader.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainContent)
            detailsExpanded = !detailsExpanded
            updateSection(detailsHeader, detailsTitle, detailsChevron, detailsBody, detailsExpanded)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_DETAILS_EXPANDED, detailsExpanded)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        CurrentOverlayService.setAppForeground(true)
        currentUnit = CurrentUnitPreferences.load(this)
        detailsCard.visibility = if (DisplayPreferences.showBatteryDetails(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        handler.post(refreshTask)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        CurrentOverlayService.setAppForeground(false)
        super.onPause()
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
        private const val STATE_DETAILS_EXPANDED = "details_expanded"
        private const val LEGACY_USAGE_JOB_ID = 0x424e4701
    }
}
