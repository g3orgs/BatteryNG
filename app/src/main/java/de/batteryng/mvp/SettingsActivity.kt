package de.batteryng.mvp

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
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
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** User controls for display, overlay, and app information. */
class SettingsActivity : BatteryNgActivity() {
    private lateinit var overlayStyle: OverlayStyle
    private lateinit var appearanceSettings: AppearanceSettings
    private lateinit var overlayButton: Button
    private lateinit var showBatteryDetailsCheck: CheckBox
    private lateinit var onlyPluggedCheck: CheckBox
    private lateinit var currentUnitSpinner: Spinner
    private lateinit var currentPolaritySpinner: Spinner
    private lateinit var rotationSpinner: Spinner
    private var startOverlayAfterPermission = false
    private var openOverlaySettingsAfterAppInfo = false
    private var currentUnit = CurrentUnit.MILLIAMPS
    private var currentPolarityMode = CurrentPolarityMode.AUTOMATIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        startOverlayAfterPermission = savedInstanceState?.getBoolean(STATE_PENDING_OVERLAY) ?: false
        openOverlaySettingsAfterAppInfo =
            savedInstanceState?.getBoolean(STATE_PENDING_APP_INFO) ?: false
        overlayStyle = OverlayPreferences.load(this)
        appearanceSettings = AppAppearance.load(this)
        currentUnit = CurrentUnitPreferences.load(this)
        currentPolarityMode = CurrentPolarityPreferences.loadMode(this)

        @Suppress("DEPRECATION")
        val installedVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        findViewById<TextView>(R.id.settingsVersion).text =
            getString(R.string.settings_version, installedVersion)
        val updateButton = findViewById<Button>(R.id.settingsCheckUpdatesButton)
        val updateStatus = findViewById<TextView>(R.id.settingsUpdateStatus)
        updateButton.setOnClickListener {
            checkForUpdates(installedVersion, updateButton, updateStatus)
        }
        findViewById<Button>(R.id.settingsOpenReleasesButton).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GitHubReleases.PAGE_URL)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.settings_open_releases_failed, Toast.LENGTH_LONG).show()
            }
        }

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

        findViewById<Button>(R.id.androidBatteryUsageButton).setOnClickListener {
            openAndroidBatteryUsage()
        }

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
        overlayStyle = OverlayPreferences.load(this)
        currentUnit = CurrentUnitPreferences.load(this)
        currentUnitSpinner.setSelection(currentUnit.ordinal)
        currentPolarityMode = CurrentPolarityPreferences.loadMode(this)
        currentPolaritySpinner.setSelection(currentPolarityMode.ordinal)
        showBatteryDetailsCheck.isChecked = DisplayPreferences.showBatteryDetails(this)
        updateOverlayButton()
        onlyPluggedCheck.isChecked = overlayStyle.showOnlyWhilePlugged
        rotationSpinner.setSelection(overlayStyle.rotationDegrees / 90)
        updateMeasurementInfo()
        if (openOverlaySettingsAfterAppInfo) {
            openOverlaySettingsAfterAppInfo = false
            if (Settings.canDrawOverlays(this)) startOverlay() else openOverlayPermissionSettings()
        } else if (startOverlayAfterPermission) {
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
        outState.putBoolean(STATE_PENDING_APP_INFO, openOverlaySettingsAfterAppInfo)
        super.onSaveInstanceState(outState)
    }

    private fun checkForUpdates(installedVersion: String, button: Button, status: TextView) {
        button.isEnabled = false
        status.setText(R.string.settings_update_checking)
        Thread {
            val latestVersion = runCatching { GitHubReleases.latestVersion() }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                button.isEnabled = true
                status.text = if (latestVersion == null) {
                    getString(R.string.settings_update_failed)
                } else {
                    val message = when (ReleaseVersions.compare(latestVersion, installedVersion)) {
                        1 -> R.string.settings_update_available
                        0 -> R.string.settings_update_current
                        -1 -> R.string.settings_update_ahead
                        else -> R.string.settings_update_unknown
                    }
                    when (message) {
                        R.string.settings_update_available ->
                            getString(message, latestVersion, installedVersion)
                        R.string.settings_update_current -> getString(message, latestVersion)
                        R.string.settings_update_ahead ->
                            getString(message, installedVersion, latestVersion)
                        else -> getString(message, latestVersion, installedVersion)
                    }
                }
            }
        }.start()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showOverlayPermissionHelp()
        } else {
            openOverlayPermissionSettings()
        }
    }

    private fun showOverlayPermissionHelp() {
        val restrictedSettingsPossible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        AlertDialog.Builder(this)
            .setTitle(R.string.overlay_permission_title)
            .setMessage(
                if (restrictedSettingsPossible) R.string.overlay_permission_steps_restricted
                else R.string.overlay_permission_steps
            )
            .setPositiveButton(
                if (restrictedSettingsPossible) R.string.overlay_app_info_open
                else R.string.overlay_permission_open
            ) { _, _ ->
                if (restrictedSettingsPossible) openOverlayAppInfo()
                else openOverlayPermissionSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (restrictedSettingsPossible) {
                    setNeutralButton(R.string.overlay_permission_open) { _, _ ->
                        openOverlayPermissionSettings()
                    }
                }
            }
            .show()
    }

    private fun openOverlayAppInfo() {
        openOverlaySettingsAfterAppInfo = true
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                )
            )
        } catch (_: ActivityNotFoundException) {
            openOverlaySettingsAfterAppInfo = false
            Toast.makeText(this, R.string.overlay_permission_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun openOverlayPermissionSettings() {
        startOverlayAfterPermission = true
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                // Android 11+ ignores package data and always opens the app list.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startOverlayAfterPermission = false
            Toast.makeText(this, R.string.overlay_permission_unavailable, Toast.LENGTH_LONG).show()
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

    private fun openAndroidBatteryUsage() {
        try {
            startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.android_battery_usage_unavailable, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.android_battery_usage_unavailable, Toast.LENGTH_LONG).show()
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
        private const val STATE_PENDING_APP_INFO = "pending_overlay_app_info"
    }
}
