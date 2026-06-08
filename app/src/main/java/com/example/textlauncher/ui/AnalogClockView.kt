package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.textlauncher.R
import com.example.textlauncher.domain.ClockDisplayMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.launcher_text)
        strokeCap = Paint.Cap.ROUND
    }
    private val digitalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.launcher_text)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    private var displayMode = ClockDisplayMode.Analog

    private val tickRunnable = object : Runnable {
        override fun run() {
            invalidate()
            scheduleNextTick()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleNextTick()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tickRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        if (contentWidth <= 0 || contentHeight <= 0) return

        val centerX = paddingLeft + contentWidth / 2f
        val centerY = paddingTop + contentHeight / 2f
        val radius = min(contentWidth, contentHeight) / 2f - OUTLINE_STROKE_DP.dpFloat
        val time = Calendar.getInstance()

        drawOutline(canvas, centerX, centerY, radius)
        when (displayMode) {
            ClockDisplayMode.Analog -> drawAnalogTime(canvas, centerX, centerY, radius, time)
            ClockDisplayMode.Digital -> drawDigitalTime(canvas, centerX, centerY, radius, time)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !isWithinClockHitArea(event.x, event.y)) {
            return false
        }
        return super.onTouchEvent(event)
    }

    fun setDisplayMode(mode: ClockDisplayMode) {
        if (displayMode == mode) return
        displayMode = mode
        invalidate()
        scheduleNextTick()
    }

    fun nextDisplayMode(): ClockDisplayMode {
        return when (displayMode) {
            ClockDisplayMode.Analog -> ClockDisplayMode.Digital
            ClockDisplayMode.Digital -> ClockDisplayMode.Analog
        }
    }

    private fun drawOutline(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        clockPaint.style = Paint.Style.STROKE
        clockPaint.strokeWidth = OUTLINE_STROKE_DP.dpFloat
        canvas.drawCircle(centerX, centerY, radius, clockPaint)
    }

    private fun drawAnalogTime(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        time: Calendar,
    ) {
        val hour = time.get(Calendar.HOUR)
        val minute = time.get(Calendar.MINUTE)
        val second = time.get(Calendar.SECOND)

        drawHand(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY,
            angleDegrees = (hour + minute / 60f) * HOUR_DEGREES,
            length = radius * HOUR_HAND_LENGTH,
            strokeWidth = HOUR_HAND_STROKE_DP.dpFloat,
        )
        drawHand(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY,
            angleDegrees = (minute + second / 60f) * MINUTE_DEGREES,
            length = radius * MINUTE_HAND_LENGTH,
            strokeWidth = MINUTE_HAND_STROKE_DP.dpFloat,
        )
        drawHand(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY,
            angleDegrees = second * SECOND_DEGREES,
            length = radius * SECOND_HAND_LENGTH,
            strokeWidth = SECOND_HAND_STROKE_DP.dpFloat,
        )

        clockPaint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, CENTER_DOT_RADIUS_DP.dpFloat, clockPaint)
    }

    private fun drawDigitalTime(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        time: Calendar,
    ) {
        digitalPaint.textSize = min(radius * DIGITAL_TEXT_SIZE_RATIO, MAX_DIGITAL_TEXT_SIZE_DP.dpFloat)
        val baseline = centerY - (digitalPaint.ascent() + digitalPaint.descent()) / 2f
        val hourText = HOUR_FORMATTER.format(time.time)
        val minuteText = MINUTE_FORMATTER.format(time.time)
        val colonText = ":"
        val hourWidth = digitalPaint.measureText(hourText)
        val colonWidth = digitalPaint.measureText(colonText)
        val minuteWidth = digitalPaint.measureText(minuteText)
        val totalWidth = hourWidth + colonWidth + minuteWidth
        var textX = centerX - totalWidth / 2f

        digitalPaint.textAlign = Paint.Align.LEFT
        digitalPaint.alpha = FULL_ALPHA
        canvas.drawText(hourText, textX, baseline, digitalPaint)
        textX += hourWidth

        digitalPaint.alpha = pulsingColonAlpha(time)
        canvas.drawText(colonText, textX, baseline, digitalPaint)
        textX += colonWidth

        digitalPaint.alpha = FULL_ALPHA
        canvas.drawText(minuteText, textX, baseline, digitalPaint)
        digitalPaint.textAlign = Paint.Align.CENTER
    }

    private fun pulsingColonAlpha(time: Calendar): Int {
        val phase = (time.get(Calendar.MILLISECOND) + time.get(Calendar.SECOND) * ONE_SECOND_MS) %
            COLON_PULSE_PERIOD_MS
        val wave = (1f + cos(phase.toFloat() / COLON_PULSE_PERIOD_MS * FULL_TURN_RADIANS)) / 2f
        return (COLON_MIN_ALPHA + (COLON_MAX_ALPHA - COLON_MIN_ALPHA) * wave).toInt()
    }

    private fun drawHand(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        angleDegrees: Float,
        length: Float,
        strokeWidth: Float,
    ) {
        val radians = Math.toRadians((angleDegrees - QUARTER_TURN_DEGREES).toDouble())
        clockPaint.style = Paint.Style.STROKE
        clockPaint.strokeWidth = strokeWidth
        canvas.drawLine(
            centerX,
            centerY,
            centerX + cos(radians).toFloat() * length,
            centerY + sin(radians).toFloat() * length,
            clockPaint,
        )
    }

    private fun isWithinClockHitArea(x: Float, y: Float): Boolean {
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        if (contentWidth <= 0 || contentHeight <= 0) return false

        val centerX = paddingLeft + contentWidth / 2f
        val centerY = paddingTop + contentHeight / 2f
        val radius = min(contentWidth, contentHeight) / 2f - OUTLINE_STROKE_DP.dpFloat
        if (radius <= 0f) return false

        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= radius * radius
    }

    private fun scheduleNextTick() {
        removeCallbacks(tickRunnable)
        if (displayMode == ClockDisplayMode.Digital) {
            postInvalidateOnAnimation()
            postOnAnimation(tickRunnable)
        } else {
            postDelayed(tickRunnable, ONE_SECOND_MS)
        }
    }

    private val Int.dpFloat: Float
        get() = this * resources.displayMetrics.density

    private companion object {
        val HOUR_FORMATTER = SimpleDateFormat("HH", Locale.getDefault())
        val MINUTE_FORMATTER = SimpleDateFormat("mm", Locale.getDefault())
        const val FULL_ALPHA = 255
        const val ONE_SECOND_MS = 1_000L
        const val COLON_PULSE_PERIOD_MS = 2_400L
        const val FULL_TURN_RADIANS = (Math.PI * 2).toFloat()
        const val COLON_MIN_ALPHA = 70
        const val COLON_MAX_ALPHA = 210
        const val QUARTER_TURN_DEGREES = 90f
        const val HOUR_DEGREES = 30f
        const val MINUTE_DEGREES = 6f
        const val SECOND_DEGREES = 6f
        const val HOUR_HAND_LENGTH = 0.45f
        const val MINUTE_HAND_LENGTH = 0.68f
        const val SECOND_HAND_LENGTH = 0.78f
        const val OUTLINE_STROKE_DP = 2
        const val HOUR_HAND_STROKE_DP = 4
        const val MINUTE_HAND_STROKE_DP = 3
        const val SECOND_HAND_STROKE_DP = 1
        const val CENTER_DOT_RADIUS_DP = 3
        const val DIGITAL_TEXT_SIZE_RATIO = 0.42f
        const val MAX_DIGITAL_TEXT_SIZE_DP = 34
    }
}
