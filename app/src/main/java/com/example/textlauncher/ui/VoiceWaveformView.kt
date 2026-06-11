package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.textlauncher.R
import kotlin.math.max

class VoiceWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var samples: List<Int> = emptyList()
        set(value) {
            field = value.map { it.coerceIn(0, 100) }
            invalidate()
        }
    var progressFraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val remainingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.launcher_text_secondary)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.resources.displayMetrics.density * 2f
    }
    private val playedPaint = Paint(remainingPaint).apply {
        color = ContextCompat.getColor(context, R.color.launcher_text)
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.launcher_text)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val visibleSamples = samples.ifEmpty { DEFAULT_SAMPLES }
        if (width <= 0 || height <= 0) return

        val step = width.toFloat() / max(visibleSamples.size, 1)
        val progressX = width * progressFraction
        visibleSamples.forEachIndexed { index, sample ->
            val x = step * index + step / 2f
            val normalized = sample.coerceIn(8, 100) / 100f
            val halfHeight = height * normalized * 0.42f
            val centerY = height / 2f
            val paint = if (x <= progressX) playedPaint else remainingPaint
            canvas.drawLine(x, centerY - halfHeight, x, centerY + halfHeight, paint)
        }
        if (progressFraction > 0f) {
            val radius = context.resources.displayMetrics.density * 4f
            val x = progressX.coerceIn(radius, width - radius)
            canvas.drawCircle(x, height / 2f, radius, playheadPaint)
        }
    }

    private companion object {
        val DEFAULT_SAMPLES = listOf(20, 34, 26, 52, 42, 72, 38, 56, 24, 44, 30, 62)
    }
}
