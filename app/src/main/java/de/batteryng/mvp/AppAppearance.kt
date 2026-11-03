package de.batteryng.mvp

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import java.util.Locale

enum class AppLanguage { SYSTEM, GERMAN, ENGLISH }

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class AppearanceSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: AppTheme = AppTheme.SYSTEM,
)

/** Keeps display preferences independent of the usage and overlay settings. */
object AppAppearance {
    private const val PREFS = "batteryng_appearance"
    private const val LANGUAGE = "language"
    private const val THEME = "theme"

    fun load(context: Context): AppearanceSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val language = runCatching {
            AppLanguage.valueOf(prefs.getString(LANGUAGE, AppLanguage.SYSTEM.name)!!)
        }.getOrDefault(AppLanguage.SYSTEM)
        val theme = runCatching {
            AppTheme.valueOf(prefs.getString(THEME, AppTheme.SYSTEM.name)!!)
        }.getOrDefault(AppTheme.SYSTEM)
        return AppearanceSettings(language, theme)
    }

    fun save(context: Context, settings: AppearanceSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LANGUAGE, settings.language.name)
            .putString(THEME, settings.theme.name)
            .apply()
    }

    fun wrap(base: Context, settings: AppearanceSettings = load(base)): Context {
        val configuration = Configuration(base.resources.configuration)
        val deviceLocale = if (Build.VERSION.SDK_INT >= 24) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        val selectedLocale = when (settings.language) {
            AppLanguage.SYSTEM -> deviceLocale
            AppLanguage.GERMAN -> Locale.GERMAN
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        Locale.setDefault(selectedLocale)
        if (settings.language != AppLanguage.SYSTEM) {
            configuration.setLocale(selectedLocale)
            configuration.setLayoutDirection(selectedLocale)
        }
        val nightMode = when (settings.theme) {
            AppTheme.SYSTEM -> null
            AppTheme.LIGHT -> Configuration.UI_MODE_NIGHT_NO
            AppTheme.DARK -> Configuration.UI_MODE_NIGHT_YES
        }
        if (nightMode != null) {
            configuration.uiMode =
                (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        return base.createConfigurationContext(configuration)
    }
}

/** Applies selected locale and theme before any screen inflates its views. */
open class BatteryNgActivity : Activity() {
    private lateinit var appliedAppearance: AppearanceSettings

    override fun attachBaseContext(newBase: Context) {
        appliedAppearance = AppAppearance.load(newBase)
        super.attachBaseContext(AppAppearance.wrap(newBase, appliedAppearance))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        setTheme(if (dark) R.style.Theme_BatteryNG_Dark else R.style.Theme_BatteryNG)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (AppAppearance.load(this) != appliedAppearance) recreate()
    }
}
