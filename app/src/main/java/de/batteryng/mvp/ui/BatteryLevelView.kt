package de.batteryng.mvp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import de.batteryng.mvp.R
import kotlin.math.min
import kotlin.math.roundToInt

/** Compact battery outline whose interior reflects the reported charge percentage. */
class BatteryLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val body = RectF()
    private val terminal = RectF()
    private val charge = RectF()
    private var levelPercent: Double? = null
    private var tintColor: Int = context.getColor(R.color.primary)

    init {
        // The adjacent percentage text announces the same information to screen readers.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** A null level leaves the icon hollow. Values outside 0–100 are clamped. */
    fun setLevelPercent(level: Double?) {
        val normalized = level?.takeIf { it.isFinite() }?.coerceIn(0.0, 100.0)
        if (levelPercent == normalized) return
        levelPercent = normalized
        invalidate()
    }

    /** Use the current card's accent so the icon remains clear in either theme. */
    fun setTintColor(color: Int) {
        if (tintColor == color) return
        tintColor = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(34f).roundToInt() + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(dp(22f).roundToInt() + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        val availableWidth = right - left
        val availableHeight = bottom - top
        if (availableWidth <= 0f || availableHeight <= 0f) return

        val stroke = dp(1.8f)
        val capWidth = min(dp(3.5f), availableWidth * 0.12f)
        val capGap = dp(1.5f)
        val bodyRight = right - capWidth - capGap - stroke / 2f
        val bodyLeft = left + stroke / 2f
        val bodyHeight = min(dp(18f), availableHeight - stroke)
        if (bodyRight <= bodyLeft || bodyHeight <= stroke) return
        val middleY = (top + bottom) / 2f
        body.set(bodyLeft, middleY - bodyHeight / 2f, bodyRight, middleY + bodyHeight / 2f)

        outlinePaint.strokeWidth = stroke
        outlinePaint.color = tintColor
        canvas.drawRoundRect(body, dp(3.2f), dp(3.2f), outlinePaint)

        terminal.set(
            bodyRight + stroke / 2f + capGap,
            middleY - bodyHeight * 0.22f,
            right,
            middleY + bodyHeight * 0.22f,
        )
        fillPaint.color = tintColor
        canvas.drawRoundRect(terminal, dp(1f), dp(1f), fillPaint)

        val fraction = (levelPercent ?: 0.0).toFloat() / 100f
        if (fraction <= 0f) return
        val inset = stroke + dp(1.5f)
        val fillLeft = body.left + inset
        val fillRightLimit = body.right - inset
        val fillWidth = (fillRightLimit - fillLeft) * fraction
        if (fillWidth <= 0f) return
        charge.set(fillLeft, body.top + inset, fillLeft + fillWidth, body.bottom - inset)
        val radius = min(dp(1.5f), fillWidth / 2f)
        canvas.drawRoundRect(charge, radius, radius, fillPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
