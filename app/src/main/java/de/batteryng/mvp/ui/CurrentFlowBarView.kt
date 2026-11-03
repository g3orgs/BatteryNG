package de.batteryng.mvp.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import de.batteryng.mvp.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Five short segments on either side of zero; net drain moves left, charge moves right. */
class CurrentFlowBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectangle = RectF()
    private val fillRectangle = RectF()
    private val animationTick = Runnable { invalidate() }

    private var chargingColor = Color.GREEN
    private var dischargingColor = Color.RED
    private var centerColor = Color.GRAY
    private var currentMilliAmps: Double? = null
    private var animatedSegments = 0f
    private var scaleMilliAmps = DEFAULT_SCALE_MILLIAMPS
    private var valueAnimator: ValueAnimator? = null

    init {
        refreshPalette()
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** The bar's outer ends correspond to +/− [maxAbsoluteMilliAmps]. */
    fun setScaleMilliAmps(maxAbsoluteMilliAmps: Double) {
        if (!maxAbsoluteMilliAmps.isFinite() || maxAbsoluteMilliAmps <= 0.0) return
        scaleMilliAmps = maxAbsoluteMilliAmps
        setCurrentMilliAmps(currentMilliAmps)
    }

    /** Call on the UI thread whenever a fresh signed net-current reading is available. */
    fun setCurrentMilliAmps(value: Double?) {
        currentMilliAmps = value?.takeIf { it.isFinite() }
        val target = currentMilliAmps?.let {
            (it / scaleMilliAmps * SEGMENTS_PER_SIDE).coerceIn(
                -SEGMENTS_PER_SIDE.toDouble(), SEGMENTS_PER_SIDE.toDouble(),
            ).toFloat()
        } ?: 0f
        if (abs(target - animatedSegments) < 0.01f) {
            animatedSegments = target
            invalidate()
            return
        }
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofFloat(animatedSegments, target).apply {
            duration = VALUE_ANIMATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedSegments = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Re-read the active light or dark theme colors after a theme change. */
    fun refreshPalette() {
        chargingColor = context.getColor(R.color.charging)
        dischargingColor = context.getColor(R.color.discharging)
        centerColor = context.getColor(R.color.neutral)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshPalette()
    }

    override fun onDetachedFromWindow() {
        valueAnimator?.cancel()
        valueAnimator = null
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) invalidate() else removeCallbacks(animationTick)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dp(280f).roundToInt() + paddingLeft + paddingRight
        val desiredHeight = dp(38f).roundToInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            View.resolveSize(desiredWidth, widthMeasureSpec),
            View.resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        val centerX = (left + right) / 2f
        val centerGap = dp(16f)
        val segmentGap = dp(5f)
        val segmentWidth = ((right - left - centerGap) / 2f -
            (SEGMENTS_PER_SIDE - 1) * segmentGap) / SEGMENTS_PER_SIDE
        if (segmentWidth <= 0f || bottom <= top) return
        val segmentHeight = min(dp(13f), bottom - top)
        val segmentTop = (top + bottom - segmentHeight) / 2f
        val segmentBottom = segmentTop + segmentHeight
        val radius = segmentHeight / 2f
        val activeLeft = max(0f, -animatedSegments)
        val activeRight = max(0f, animatedSegments)
        val phase = (SystemClock.uptimeMillis() % FLOW_ANIMATION_MS).toFloat() /
            FLOW_ANIMATION_MS

        drawSide(
            canvas, centerX - centerGap / 2f, segmentWidth, segmentGap,
            segmentTop, segmentBottom, radius, activeLeft, phase,
            dischargingColor, toLeft = true,
        )
        drawSide(
            canvas, centerX + centerGap / 2f, segmentWidth, segmentGap,
            segmentTop, segmentBottom, radius, activeRight, phase,
            chargingColor, toLeft = false,
        )

        centerPaint.color = withOpacity(centerColor, if (currentMilliAmps == null) 0.42f else 0.82f)
        rectangle.set(centerX - dp(1.2f), segmentTop - dp(3f),
            centerX + dp(1.2f), segmentBottom + dp(3f))
        canvas.drawRoundRect(rectangle, dp(1.2f), dp(1.2f), centerPaint)

        if (isShown && abs(animatedSegments) > 0.01f) {
            removeCallbacks(animationTick)
            postDelayed(animationTick, FLOW_FRAME_MS)
        }
    }

    private fun drawSide(
        canvas: Canvas,
        innerEdge: Float,
        segmentWidth: Float,
        segmentGap: Float,
        segmentTop: Float,
        segmentBottom: Float,
        radius: Float,
        activeSegments: Float,
        phase: Float,
        color: Int,
        toLeft: Boolean,
    ) {
        val glintPosition = phase * max(1f, ceil(activeSegments))
        for (index in 0 until SEGMENTS_PER_SIDE) {
            val segmentLeft = if (toLeft) {
                innerEdge - segmentWidth - index * (segmentWidth + segmentGap)
            } else {
                innerEdge + index * (segmentWidth + segmentGap)
            }
            rectangle.set(segmentLeft, segmentTop,
                segmentLeft + segmentWidth, segmentBottom)

            val filledFraction = (activeSegments - index).coerceIn(0f, 1f)
            trackPaint.color = withOpacity(color, 0.09f)
            canvas.drawRoundRect(rectangle, radius, radius, trackPaint)

            if (filledFraction > 0f) {
                val filledWidth = segmentWidth * filledFraction
                if (toLeft) {
                    fillRectangle.set(rectangle.right - filledWidth, segmentTop,
                        rectangle.right, segmentBottom)
                } else {
                    fillRectangle.set(rectangle.left, segmentTop,
                        rectangle.left + filledWidth, segmentBottom)
                }
                val fillRadius = min(radius, filledWidth / 2f)
                fillPaint.color = color
                canvas.drawRoundRect(fillRectangle, fillRadius, fillRadius, fillPaint)

                val waveDistance = abs((index + filledFraction / 2f) - glintPosition)
                val shimmer = (1f - waveDistance).coerceIn(0f, 1f)
                if (shimmer > 0f) {
                    glintPaint.color = withOpacity(Color.WHITE, 0.20f * shimmer)
                    canvas.drawRoundRect(fillRectangle, fillRadius, fillRadius, glintPaint)
                }
            }

            outlinePaint.color = withOpacity(color, if (filledFraction > 0f) 0.90f else 0.45f)
            outlinePaint.strokeWidth = dp(1.1f)
            val inset = outlinePaint.strokeWidth / 2f
            rectangle.inset(inset, inset)
            canvas.drawRoundRect(rectangle, radius, radius, outlinePaint)
        }
    }

    private fun withOpacity(color: Int, opacity: Float): Int = Color.argb(
        (Color.alpha(color) * opacity.coerceIn(0f, 1f)).roundToInt(),
        Color.red(color), Color.green(color), Color.blue(color),
    )

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val SEGMENTS_PER_SIDE = 5
        private const val DEFAULT_SCALE_MILLIAMPS = 2_500.0
        private const val VALUE_ANIMATION_MS = 450L
        private const val FLOW_ANIMATION_MS = 1_350L
        private const val FLOW_FRAME_MS = 90L
    }
}
