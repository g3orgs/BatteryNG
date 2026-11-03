package de.batteryng.mvp

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import de.batteryng.mvp.overlay.CurrentOverlayService
import de.batteryng.mvp.overlay.OverlayPreferences
import de.batteryng.mvp.overlay.OverlayStyle

/** Live controls for the appearance of the floating readout. */
object OverlaySettingsDialog {
    fun show(activity: Activity) {
        fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()

        var style = OverlayPreferences.load(activity)
        fun save(next: OverlayStyle) {
            style = next
            OverlayPreferences.save(activity, style)
            CurrentOverlayService.refreshAppearance()
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(16))
        }
        val scroll = ScrollView(activity).apply {
            addView(content)
        }

        fun label(text: String, topMargin: Int = 12): TextView {
            val view = TextView(activity).apply {
                this.text = text
                textSize = 16f
                setTextColor(activity.getColor(R.color.primary))
            }
            content.addView(view, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { this.topMargin = dp(topMargin) })
            return view
        }

        val fontLabel = label(activity.getString(R.string.overlay_font_size, style.fontSizeSp))
        val fontSlider = SeekBar(activity).apply {
            max = 30
            progress = style.fontSizeSp - 10
            contentDescription = activity.getString(R.string.overlay_font_size, style.fontSizeSp)
        }
        content.addView(fontSlider)
        fontSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val size = progress + 10
                fontLabel.text = activity.getString(R.string.overlay_font_size, size)
                fontSlider.contentDescription = fontLabel.text
                save(style.copy(fontSizeSp = size))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        fun colorRow(title: String, color: () -> Int, onColor: (Int) -> Unit) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                contentDescription = title
                setPadding(0, dp(12), 0, dp(12))
            }
            val name = TextView(activity).apply {
                text = title
                textSize = 16f
                setTextColor(activity.getColor(R.color.primary))
            }
            row.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val swatch = View(activity)
            fun updateSwatch() {
                swatch.background = GradientDrawable().apply {
                    setColor(color())
                    setStroke(dp(1), Color.GRAY)
                }
            }
            updateSwatch()
            row.addView(swatch, LinearLayout.LayoutParams(dp(48), dp(32)))
            row.setOnClickListener {
                showColorPicker(activity, title, color()) { selected ->
                    onColor(selected)
                    updateSwatch()
                }
            }
            content.addView(row)
        }

        colorRow(activity.getString(R.string.overlay_text_color), { style.textColor }) { selected ->
            save(style.copy(textColor = selected))
        }
        colorRow(activity.getString(R.string.overlay_background_color), { style.backgroundColor }) { selected ->
            save(style.copy(backgroundColor = selected))
        }

        val opacityLabel = label(
            activity.getString(R.string.overlay_background_opacity, style.backgroundOpacityPercent),
        )
        val opacitySlider = SeekBar(activity).apply {
            max = 100
            progress = style.backgroundOpacityPercent
            contentDescription = opacityLabel.text
        }
        content.addView(opacitySlider)
        opacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                opacityLabel.text = activity.getString(R.string.overlay_background_opacity, progress)
                opacitySlider.contentDescription = opacityLabel.text
                save(style.copy(backgroundOpacityPercent = progress))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        label(activity.getString(R.string.overlay_drag_explanation), 20).apply {
            textSize = 14f
            setTextColor(activity.getColor(R.color.secondary))
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.overlay_style_title)
            .setView(scroll)
            .setPositiveButton(R.string.overlay_style_done, null)
            .show()
    }
}
