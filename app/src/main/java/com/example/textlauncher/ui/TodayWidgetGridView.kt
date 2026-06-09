package com.example.textlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlin.math.roundToInt

class TodayWidgetGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    var isEditingWidgets: Boolean = false
        set(value) {
            field = value
        }

    fun applyGridPosition(view: View, column: Int, row: Int, columnSpan: Int, rowSpan: Int) {
        if (!hasGridSize) return

        val safeColumnSpan = columnSpan.coerceIn(MIN_COLUMN_SPAN, COLUMN_COUNT)
        val safeRowSpan = rowSpan.coerceIn(MIN_ROW_SPAN, ROW_COUNT)
        val safeColumn = columnForDrag(column, safeColumnSpan)
        val safeRow = rowForDrag(row, safeRowSpan)
        val left = safeColumn * cellWidth
        val top = safeRow * cellHeight
        val right = (safeColumn + safeColumnSpan) * cellWidth
        val bottom = (safeRow + safeRowSpan) * cellHeight
        view.layoutParams = LayoutParams(
            (right - left).roundToInt().coerceAtLeast(1),
            (bottom - top).roundToInt().coerceAtLeast(1),
        ).apply {
            leftMargin = left.roundToInt()
            topMargin = top.roundToInt()
        }
    }

    fun columnForDrag(rawColumn: Int, columnSpan: Int): Int {
        val safeColumnSpan = columnSpan.coerceIn(MIN_COLUMN_SPAN, COLUMN_COUNT)
        return rawColumn.coerceIn(0, (COLUMN_COUNT - safeColumnSpan).coerceAtLeast(0))
    }

    fun rowForDrag(rawRow: Int, rowSpan: Int): Int {
        val safeRowSpan = rowSpan.coerceIn(MIN_ROW_SPAN, ROW_COUNT)
        return rawRow.coerceIn(0, (ROW_COUNT - safeRowSpan).coerceAtLeast(0))
    }

    fun columnDeltaForX(deltaX: Float): Int {
        if (!hasGridSize) return 0
        return (deltaX / cellWidth).roundToInt()
    }

    fun rowDeltaForY(deltaY: Float): Int {
        if (!hasGridSize) return 0
        return (deltaY / cellHeight).roundToInt()
    }

    fun columnSpanForWidth(widthPx: Float): Int {
        if (!hasGridSize) return MIN_COLUMN_SPAN
        return (widthPx / cellWidth).roundToInt().coerceIn(MIN_COLUMN_SPAN, COLUMN_COUNT)
    }

    fun rowSpanForHeight(heightPx: Float): Int {
        if (!hasGridSize) return MIN_ROW_SPAN
        return (heightPx / cellHeight).roundToInt().coerceIn(MIN_ROW_SPAN, ROW_COUNT)
    }

    private val hasGridSize: Boolean
        get() = contentWidth > 0 && contentHeight > 0

    private val cellWidth: Float
        get() = contentWidth.toFloat() / COLUMN_COUNT

    private val cellHeight: Float
        get() = contentHeight.toFloat() / ROW_COUNT

    private val contentWidth: Int
        get() = width - paddingLeft - paddingRight

    private val contentHeight: Int
        get() = height - paddingTop - paddingBottom

    companion object {
        const val COLUMN_COUNT = 4
        const val ROW_COUNT = 6
        const val MIN_COLUMN_SPAN = 2
        const val MIN_ROW_SPAN = 1
    }
}
