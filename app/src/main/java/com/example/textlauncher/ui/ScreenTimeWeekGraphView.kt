package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.textlauncher.R
import com.example.textlauncher.domain.ScreenTimeDayUsage

class ScreenTimeWeekGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.launcher_text)
    }
    private val futureBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.settings_option_divider)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.launcher_text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 12.sp
    }
    private val barRect = RectF()
    private var weekUsage: List<ScreenTimeDayUsage> = emptyList()

    fun setWeekUsage(usage: List<ScreenTimeDayUsage>) {
        weekUsage = usage
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (weekUsage.isEmpty()) return

        val graphTop = paddingTop.toFloat()
        val labelBaseline = height - paddingBottom - LABEL_BOTTOM_GAP_DP.dp.toFloat()
        val graphBottom = labelBaseline - LABEL_TOP_GAP_DP.dp
        val graphHeight = (graphBottom - graphTop).coerceAtLeast(1f)
        val slotWidth = (width - paddingStart - paddingEnd).toFloat() / weekUsage.size
        val maxUsage = weekUsage.maxOf { it.usageMillis }.coerceAtLeast(1L)
        val barWidth = minOf(MAX_BAR_WIDTH_DP.dp.toFloat(), slotWidth * BAR_WIDTH_FRACTION)

        weekUsage.forEachIndexed { index, dayUsage ->
            val centerX = paddingStart + slotWidth * index + slotWidth / 2f
            val heightFraction = dayUsage.usageMillis.toFloat() / maxUsage
            val barHeight = (graphHeight * heightFraction).coerceAtLeast(if (dayUsage.usageMillis > 0) MIN_BAR_HEIGHT_DP.dp.toFloat() else 0f)
            barRect.set(
                centerX - barWidth / 2f,
                graphBottom - barHeight,
                centerX + barWidth / 2f,
                graphBottom,
            )
            canvas.drawRoundRect(
                barRect,
                BAR_RADIUS_DP.dp.toFloat(),
                BAR_RADIUS_DP.dp.toFloat(),
                if (dayUsage.isElapsed) barPaint else futureBarPaint,
            )
            canvas.drawText(dayUsage.label.take(1), centerX, labelBaseline, labelPaint)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private val Int.sp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            toFloat(),
            resources.displayMetrics,
        )

    private companion object {
        const val BAR_WIDTH_FRACTION = 0.42f
        const val MAX_BAR_WIDTH_DP = 18
        const val MIN_BAR_HEIGHT_DP = 3
        const val BAR_RADIUS_DP = 4
        const val LABEL_TOP_GAP_DP = 14
        const val LABEL_BOTTOM_GAP_DP = 2
    }
}
