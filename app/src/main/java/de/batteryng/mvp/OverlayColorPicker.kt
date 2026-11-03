package de.batteryng.mvp

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Opens a compact RGB picker and returns an opaque color when confirmed. */
fun showColorPicker(
    activity: Activity,
    title: String,
    initialColor: Int,
    onSelected: (Int) -> Unit,
) {
    fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()

    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(8))
    }
    val preview = TextView(activity).apply {
        gravity = Gravity.CENTER
        textSize = 18f
        minHeight = dp(64)
    }
    content.addView(
        preview,
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)),
    )

    fun addChannel(name: String, initial: Int): Pair<TextView, SeekBar> {
        val label = TextView(activity).apply {
            textSize = 15f
        }
        content.addView(
            label,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
            },
        )
        val slider = SeekBar(activity).apply {
            max = 255
            progress = initial
            contentDescription = name
        }
        content.addView(
            slider,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        return label to slider
    }

    val redName = activity.getString(R.string.color_red)
    val greenName = activity.getString(R.string.color_green)
    val blueName = activity.getString(R.string.color_blue)
    val (redLabel, redSlider) = addChannel(redName, Color.red(initialColor))
    val (greenLabel, greenSlider) = addChannel(greenName, Color.green(initialColor))
    val (blueLabel, blueSlider) = addChannel(blueName, Color.blue(initialColor))

    fun selectedColor(): Int = Color.rgb(redSlider.progress, greenSlider.progress, blueSlider.progress)

    fun updatePreview() {
        val color = selectedColor()
        redLabel.text = activity.getString(R.string.color_channel_value, redName, redSlider.progress)
        greenLabel.text = activity.getString(R.string.color_channel_value, greenName, greenSlider.progress)
        blueLabel.text = activity.getString(R.string.color_channel_value, blueName, blueSlider.progress)
        preview.text = String.format("#%06X", color and 0xFFFFFF)
        preview.background = GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), Color.GRAY)
        }
        val brightness = Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114
        preview.setTextColor(if (brightness < 128_000) Color.WHITE else Color.BLACK)
    }

    val changeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            updatePreview()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }
    redSlider.setOnSeekBarChangeListener(changeListener)
    greenSlider.setOnSeekBarChangeListener(changeListener)
    blueSlider.setOnSeekBarChangeListener(changeListener)
    updatePreview()

    AlertDialog.Builder(activity)
        .setTitle(title)
        .setView(content)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> onSelected(selectedColor()) }
        .show()
}
