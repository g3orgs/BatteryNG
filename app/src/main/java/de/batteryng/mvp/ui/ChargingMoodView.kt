package de.batteryng.mvp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Compact, expression-first indicator for the net current entering the battery. */
class ChargingMoodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mouthPath = Path()
    private var mood: ChargingMood? = null

    fun setMood(next: ChargingMood?) {
        if (mood == next) return
        mood = next
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentMood = mood ?: return
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)

        paint.style = Paint.Style.FILL
        paint.color = when (currentMood) {
            ChargingMood.SAD -> Color.rgb(255, 112, 105)
            ChargingMood.SLOW -> Color.rgb(255, 171, 91)
            ChargingMood.OKAY -> Color.rgb(255, 214, 112)
            ChargingMood.GOOD -> Color.rgb(152, 224, 129)
            ChargingMood.LAUGHING -> Color.rgb(79, 224, 168)
        }
        canvas.drawCircle(size * 0.5f, size * 0.5f, size * 0.44f, paint)

        paint.color = Color.rgb(23, 49, 52)
        canvas.drawCircle(size * 0.365f, size * 0.40f, size * 0.043f, paint)
        canvas.drawCircle(size * 0.635f, size * 0.40f, size * 0.043f, paint)

        if (currentMood == ChargingMood.LAUGHING) {
            canvas.drawOval(size * 0.33f, size * 0.55f, size * 0.67f, size * 0.81f, paint)
            paint.color = Color.WHITE
            canvas.drawRoundRect(
                size * 0.39f, size * 0.55f, size * 0.61f, size * 0.61f,
                size * 0.02f, size * 0.02f, paint,
            )
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.075f
            paint.strokeCap = Paint.Cap.ROUND
            mouthPath.reset()
            when (currentMood) {
                ChargingMood.SAD -> {
                    mouthPath.moveTo(size * 0.32f, size * 0.69f)
                    mouthPath.quadTo(size * 0.50f, size * 0.47f, size * 0.68f, size * 0.69f)
                }
                ChargingMood.SLOW -> {
                    mouthPath.moveTo(size * 0.34f, size * 0.64f)
                    mouthPath.lineTo(size * 0.66f, size * 0.64f)
                }
                ChargingMood.OKAY -> {
                    mouthPath.moveTo(size * 0.32f, size * 0.60f)
                    mouthPath.quadTo(size * 0.50f, size * 0.73f, size * 0.68f, size * 0.60f)
                }
                ChargingMood.GOOD -> {
                    mouthPath.moveTo(size * 0.29f, size * 0.57f)
                    mouthPath.quadTo(size * 0.50f, size * 0.84f, size * 0.71f, size * 0.57f)
                }
                ChargingMood.LAUGHING -> Unit
            }
            canvas.drawPath(mouthPath, paint)
        }
        canvas.restore()
    }
}
