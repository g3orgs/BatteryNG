package de.batteryng.mvp.overlay

import android.content.Context
import android.graphics.Color

/** Display choices for the floating battery-current readout. */
data class OverlayStyle(
    val fontSizeSp: Int = 16,
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.rgb(15, 36, 63),
    /** 0 is invisible; 100 is the fully opaque selected color. */
    val backgroundOpacityPercent: Int = 40,
    /** Clockwise rotation of the complete floating readout. */
    val rotationDegrees: Int = 0,
    /** Keep the overlay active but hide its window until external power is connected. */
    val showOnlyWhilePlugged: Boolean = false,
)

object OverlayPreferences {
    private const val PREFS_NAME = "batteryng_overlay"
    private const val KEY_FONT_SIZE = "font_size_sp"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_BACKGROUND_OPACITY = "background_opacity_percent"
    private const val KEY_ROTATION_DEGREES = "rotation_degrees"
    private const val KEY_SHOW_ONLY_WHILE_PLUGGED = "show_only_while_plugged"
    private const val KEY_POSITION_X = "position_x_px"
    private const val KEY_POSITION_Y = "position_y_px"

    fun load(context: Context): OverlayStyle {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = OverlayStyle()
        return OverlayStyle(
            fontSizeSp = prefs.getInt(KEY_FONT_SIZE, defaults.fontSizeSp).coerceIn(10, 40),
            textColor = prefs.getInt(KEY_TEXT_COLOR, defaults.textColor),
            backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, defaults.backgroundColor),
            backgroundOpacityPercent = prefs.getInt(
                KEY_BACKGROUND_OPACITY,
                defaults.backgroundOpacityPercent,
            ).coerceIn(0, 100),
            rotationDegrees = normalizeRotation(
                prefs.getInt(KEY_ROTATION_DEGREES, defaults.rotationDegrees),
            ),
            showOnlyWhilePlugged = prefs.getBoolean(
                KEY_SHOW_ONLY_WHILE_PLUGGED,
                defaults.showOnlyWhilePlugged,
            ),
        )
    }

    fun save(context: Context, style: OverlayStyle) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_FONT_SIZE, style.fontSizeSp.coerceIn(10, 40))
            .putInt(KEY_TEXT_COLOR, style.textColor)
            .putInt(KEY_BACKGROUND_COLOR, style.backgroundColor)
            .putInt(KEY_BACKGROUND_OPACITY, style.backgroundOpacityPercent.coerceIn(0, 100))
            .putInt(KEY_ROTATION_DEGREES, normalizeRotation(style.rotationDegrees))
            .putBoolean(KEY_SHOW_ONLY_WHILE_PLUGGED, style.showOnlyWhilePlugged)
            .apply()
    }

    private fun normalizeRotation(degrees: Int): Int = when (degrees) {
        0, 90, 180, 270 -> degrees
        else -> 0
    }

    internal fun loadPosition(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_POSITION_X) || !prefs.contains(KEY_POSITION_Y)) return null
        return prefs.getInt(KEY_POSITION_X, 0) to prefs.getInt(KEY_POSITION_Y, 0)
    }

    internal fun savePosition(context: Context, xPx: Int, yPx: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_POSITION_X, xPx)
            .putInt(KEY_POSITION_Y, yPx)
            .apply()
    }
}
