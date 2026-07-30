package com.example.textlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isGone
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
        val safeColumnSpan = columnSpan.coerceIn(MIN_COLUMN_SPAN, COLUMN_COUNT)
        val safeRowSpan = rowSpan.coerceIn(MIN_ROW_SPAN, ROW_COUNT)
        val safeColumn = columnForDrag(column, safeColumnSpan)
        val safeRow = rowForDrag(row, safeRowSpan)
        val layoutParams = (view.layoutParams as? LayoutParams) ?: LayoutParams()
        layoutParams.column = safeColumn
        layoutParams.row = safeRow
        layoutParams.columnSpan = safeColumnSpan
        layoutParams.rowSpan = safeRowSpan
        view.layoutParams = layoutParams
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!hasGridSize) return

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.isGone) continue
            val layoutParams = child.layoutParams as? LayoutParams ?: continue
            val childWidth = (layoutParams.columnSpan * cellWidth).roundToInt().coerceAtLeast(1)
            val childHeight = (layoutParams.rowSpan * cellHeight).roundToInt().coerceAtLeast(1)
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY),
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.isGone) continue
            val layoutParams = child.layoutParams as? LayoutParams ?: continue
            val childLeft = paddingLeft + (layoutParams.column * cellWidth).roundToInt()
            val childTop = paddingTop + (layoutParams.row * cellHeight).roundToInt()
            child.layout(
                childLeft,
                childTop,
                childLeft + child.measuredWidth,
                childTop + child.measuredHeight,
            )
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams()
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(layoutParams: ViewGroup.LayoutParams?): LayoutParams {
        return when (layoutParams) {
            is LayoutParams -> LayoutParams(layoutParams)
            is ViewGroup.MarginLayoutParams -> LayoutParams(layoutParams)
            null -> LayoutParams()
            else -> LayoutParams(layoutParams)
        }
    }

    override fun checkLayoutParams(layoutParams: ViewGroup.LayoutParams?): Boolean {
        return layoutParams is LayoutParams
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
        get() = (measuredWidth.takeIf { it > 0 } ?: width) - paddingLeft - paddingRight

    private val contentHeight: Int
        get() = (measuredHeight.takeIf { it > 0 } ?: height) - paddingTop - paddingBottom

    companion object {
        const val COLUMN_COUNT = 4
        const val ROW_COUNT = 6
        const val MIN_COLUMN_SPAN = 2
        const val MIN_ROW_SPAN = 1
    }

    class LayoutParams : FrameLayout.LayoutParams {
        var column: Int = 0
        var row: Int = 0
        var columnSpan: Int = MIN_COLUMN_SPAN
        var rowSpan: Int = MIN_ROW_SPAN

        constructor() : super(0, 0)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        constructor(layoutParams: ViewGroup.LayoutParams) : super(layoutParams)
        constructor(layoutParams: ViewGroup.MarginLayoutParams) : super(layoutParams)
        constructor(layoutParams: LayoutParams) : super(layoutParams) {
            column = layoutParams.column
            row = layoutParams.row
            columnSpan = layoutParams.columnSpan
            rowSpan = layoutParams.rowSpan
        }
    }
}
