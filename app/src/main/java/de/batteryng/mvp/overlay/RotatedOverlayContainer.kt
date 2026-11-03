package de.batteryng.mvp.overlay

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/** Swaps the overlay window's measured bounds when its content turns sideways. */
internal class RotatedOverlayContainer(context: Context) : FrameLayout(context) {
    var rotationDegrees: Int = 0
        set(value) {
            val normalized = when (value) {
                0, 90, 180, 270 -> value
                else -> 0
            }
            if (field == normalized) return
            field = normalized
            requestLayout()
            invalidate()
        }

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val content = getChildAt(0)
        if (content == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val natural = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        content.measure(natural, natural)
        val sideways = rotationDegrees == 90 || rotationDegrees == 270
        val width = if (sideways) content.measuredHeight else content.measuredWidth
        val height = if (sideways) content.measuredWidth else content.measuredHeight
        setMeasuredDimension(
            View.resolveSize(width, widthMeasureSpec),
            View.resolveSize(height, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val content = getChildAt(0) ?: return
        val contentLeft = (width - content.measuredWidth) / 2
        val contentTop = (height - content.measuredHeight) / 2
        content.layout(
            contentLeft,
            contentTop,
            contentLeft + content.measuredWidth,
            contentTop + content.measuredHeight,
        )
        content.rotation = rotationDegrees.toFloat()
    }
}
