package de.batteryng.mvp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import de.batteryng.mvp.measurement.BatteryCurrentReader
import de.batteryng.mvp.measurement.CurrentPolarityMode
import de.batteryng.mvp.measurement.CurrentPolarityPreferences
import de.batteryng.mvp.measurement.CurrentSource
import de.batteryng.mvp.measurement.CurrentUnit
import de.batteryng.mvp.measurement.CurrentUnitPreferences
import de.batteryng.mvp.overlay.CurrentOverlayService
import de.batteryng.mvp.overlay.OverlayPreferences
import de.batteryng.mvp.overlay.OverlayStyle
import de.batteryng.mvp.usage.UsageChargeTracker
import de.batteryng.mvp.usage.UsagePeriod
import de.batteryng.mvp.usage.UsagePreferences
import de.batteryng.mvp.usage.UsageSettings
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** All user controls for the usage ranking and floating readout. */
class SettingsActivity : BatteryNgActivity() {
    private lateinit var usageSettings: UsageSettings
    private lateinit var overlayStyle: OverlayStyle
    private lateinit var appearanceSettings: AppearanceSettings
    private lateinit var overlayButton: Button
    private lateinit var showBatteryDetailsCheck: CheckBox
    private lateinit var showTopAppsCheck: CheckBox
    private lateinit var onlyPluggedCheck: CheckBox
    private lateinit var currentUnitSpinner: Spinner
    private lateinit var currentPolaritySpinner: Spinner
    private lateinit var rotationSpinner: Spinner
    private lateinit var topCountSeek: SeekBar
    private lateinit var topCountLabel: TextView
    private lateinit var periodGroup: RadioGroup
    private var startOverlayAfterPermission = false
    private var currentUnit = CurrentUnit.MILLIAMPS
    private var currentPolarityMode = CurrentPolarityMode.AUTOMATIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        startOverlayAfterPermission = savedInstanceState?.getBoolean(STATE_PENDING_OVERLAY) ?: false
        usageSettings = UsagePreferences.load(this)
        overlayStyle = OverlayPreferences.load(this)
        appearanceSettings = AppAppearance.load(this)
        currentUnit = CurrentUnitPreferences.load(this)
        currentPolarityMode = CurrentPolarityPreferences.loadMode(this)

        findViewById<ImageButton>(R.id.settingsBackButton).setOnClickListener { finish() }

        val languageSpinner = findViewById<Spinner>(R.id.languageSpinner)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(
                getString(R.string.settings_language_system),
                getString(R.string.settings_language_german),
                getString(R.string.settings_language_english),
            ),
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        languageSpinner.setSelection(appearanceSettings.language.ordinal, false)
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long,
            ) {
                val language = AppLanguage.entries[position]
                if (language != appearanceSettings.language) {
                    appearanceSettings = appearanceSettings.copy(language = language)
                    AppAppearance.save(this@SettingsActivity, appearanceSettings)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val themeSpinner = findViewById<Spinner>(R.id.themeSpinner)
        themeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(
                getString(R.string.settings_theme_system),
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark),
            ),
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        themeSpinner.setSelection(appearanceSettings.theme.ordinal, false)
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long,
            ) {
                val theme = AppTheme.entries[position]
                if (theme != appearanceSettings.theme) {
                    appearanceSettings = appearanceSettings.copy(theme = theme)
                    AppAppearance.save(this@SettingsActivity, appearanceSettings)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        currentUnitSpinner = findViewById(R.id.currentUnitSpinner)
        currentUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(
                getString(R.string.settings_current_unit_ma),
                getString(R.string.settings_current_unit_a),
            ),
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        currentUnitSpinner.setSelection(currentUnit.ordinal, false)
        currentUnitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long,
            ) {
                val selected = CurrentUnit.entries[position]
                if (selected != currentUnit) {
                    currentUnit = selected
                    CurrentUnitPreferences.save(this@SettingsActivity, selected)
                    CurrentOverlayService.refreshAppearance()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        currentPolaritySpinner = findViewById(R.id.currentPolaritySpinner)
        currentPolaritySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(
                getString(R.string.settings_current_polarity_automatic),
                getString(R.string.settings_current_polarity_standard),
                getString(R.string.settings_current_polarity_reversed),
            ),
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        currentPolaritySpinner.setSelection(currentPolarityMode.ordinal, false)
        currentPolaritySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long,
            ) {
                val selected = CurrentPolarityMode.entries[position]
                if (selected != currentPolarityMode) {
                    currentPolarityMode = selected
                    CurrentPolarityPreferences.saveMode(this@SettingsActivity, selected)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        showBatteryDetailsCheck = findViewById(R.id.settingsShowBatteryDetailsCheck)
        showBatteryDetailsCheck.isChecked = DisplayPreferences.showBatteryDetails(this)
        showBatteryDetailsCheck.setOnCheckedChangeListener { _, checked ->
            DisplayPreferences.setShowBatteryDetails(this, checked)
        }

        periodGroup = findViewById(R.id.usagePeriodGroup)
        periodGroup.check(
            if (usageSettings.period == UsagePeriod.LAST_24_HOURS) R.id.period24Button
            else R.id.periodChargeButton
        )
        periodGroup.setOnCheckedChangeListener { _, checkedId ->
            val period = if (checkedId == R.id.periodChargeButton) {
                UsagePeriod.SINCE_FULL_CHARGE
            } else {
                UsagePeriod.LAST_24_HOURS
            }
            if (period != usageSettings.period) saveUsage(usageSettings.copy(period = period))
        }

        topCountLabel = findViewById(R.id.topCountLabel)
        topCountSeek = findViewById(R.id.topCountSeek)
        topCountSeek.progress = usageSettings.topCount - 1
        updateTopCountLabel()
        topCountSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                saveUsage(usageSettings.copy(topCount = progress + 1))
                updateTopCountLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        showTopAppsCheck = findViewById(R.id.settingsShowTopAppsCheck)
        showTopAppsCheck.isChecked = usageSettings.showTopApps
        showTopAppsCheck.setOnCheckedChangeListener { _, checked ->
            if (checked != usageSettings.showTopApps) {
                saveUsage(usageSettings.copy(showTopApps = checked))
            }
        }
        findViewById<Button>(R.id.usageAccessButton).setOnClickListener { openUsageAccessSettings() }

        overlayButton = findViewById(R.id.settingsOverlayButton)
        overlayButton.setOnClickListener { toggleOverlay() }
        onlyPluggedCheck = findViewById(R.id.onlyPluggedCheck)
        onlyPluggedCheck.isChecked = overlayStyle.showOnlyWhilePlugged
        onlyPluggedCheck.setOnCheckedChangeListener { _, checked ->
            if (checked != overlayStyle.showOnlyWhilePlugged) {
                saveOverlay(overlayStyle.copy(showOnlyWhilePlugged = checked))
            }
        }

        rotationSpinner = findViewById(R.id.rotationSpinner)
        val rotationLabels = listOf("0°", "90°", "180°", "270°")
        rotationSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            rotationLabels,
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        rotationSpinner.setSelection(overlayStyle.rotationDegrees / 90, false)
        rotationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val degrees = position * 90
                if (degrees != overlayStyle.rotationDegrees) {
                    saveOverlay(overlayStyle.copy(rotationDegrees = degrees))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<Button>(R.id.settingsStyleButton).setOnClickListener {
            OverlaySettingsDialog.show(this)
        }
    }

    override fun onResume() {
        super.onResume()
        CurrentOverlayService.setAppForeground(true)
        UsageChargeTracker.observe(this)
        usageSettings = UsagePreferences.load(this)
        overlayStyle = OverlayPreferences.load(this)
        currentUnit = CurrentUnitPreferences.load(this)
        currentUnitSpinner.setSelection(currentUnit.ordinal)
        currentPolarityMode = CurrentPolarityPreferences.loadMode(this)
        currentPolaritySpinner.setSelection(currentPolarityMode.ordinal)
        showBatteryDetailsCheck.isChecked = DisplayPreferences.showBatteryDetails(this)
        showTopAppsCheck.isChecked = usageSettings.showTopApps
        updateOverlayButton()
        onlyPluggedCheck.isChecked = overlayStyle.showOnlyWhilePlugged
        rotationSpinner.setSelection(overlayStyle.rotationDegrees / 90)
        updateTopCountLabel()
        updateMeasurementInfo()
        if (startOverlayAfterPermission) {
            startOverlayAfterPermission = false
            if (Settings.canDrawOverlays(this)) startOverlay()
        }
    }

    override fun onPause() {
        CurrentOverlayService.setAppForeground(false)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PENDING_OVERLAY, startOverlayAfterPermission)
        super.onSaveInstanceState(outState)
    }

    private fun saveUsage(next: UsageSettings) {
        usageSettings = next
        UsagePreferences.save(this, next)
    }

    private fun updateTopCountLabel() {
        topCountLabel.text = getString(R.string.settings_top_count, usageSettings.topCount)
    }

    private fun saveOverlay(next: OverlayStyle) {
        overlayStyle = next
        OverlayPreferences.save(this, next)
        CurrentOverlayService.refreshAppearance()
    }

    private fun toggleOverlay() {
        if (CurrentOverlayService.isRunning) {
            CurrentOverlayService.stop(this)
            overlayButton.setText(R.string.overlay_start)
            return
        }
        if (Settings.canDrawOverlays(this)) {
            startOverlay()
            return
        }
        startOverlayAfterPermission = true
        Toast.makeText(this, R.string.overlay_permission_missing, Toast.LENGTH_LONG).show()
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName),
                )
            )
        } catch (_: ActivityNotFoundException) {
            startOverlayAfterPermission = false
            Toast.makeText(this, R.string.overlay_permission_missing, Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlay() {
        if (CurrentOverlayService.start(this)) {
            overlayButton.setText(R.string.overlay_stop)
        } else {
            Toast.makeText(this, R.string.overlay_start_failed, Toast.LENGTH_LONG).show()
            updateOverlayButton()
        }
    }

    private fun updateOverlayButton() {
        overlayButton.setText(
            if (CurrentOverlayService.isRunning) R.string.overlay_stop else R.string.overlay_start
        )
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.top_apps_permission_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMeasurementInfo() {
        val source = when (BatteryCurrentReader(this).read().source) {
            CurrentSource.CURRENT_NOW -> R.string.source_now
            CurrentSource.CURRENT_AVERAGE -> R.string.source_average
            CurrentSource.UNAVAILABLE -> R.string.source_unavailable
        }
        val updated = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(Date())
        findViewById<TextView>(R.id.settingsMeasurementInfo).text =
            getString(R.string.settings_measurement_source, getString(source), updated)
    }

    companion object {
        private const val STATE_PENDING_OVERLAY = "pending_overlay_start"
    }
}
