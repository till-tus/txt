package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.example.textlauncher.R
import com.example.textlauncher.domain.LauncherPage
import com.example.textlauncher.domain.PageArrangement
import com.example.textlauncher.domain.PagePosition
import kotlin.math.hypot
import kotlin.math.min

class PageArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onArrangementChanged: ((PageArrangement) -> Unit)? = null
    var onPageEnabledChanged: ((LauncherPage, Boolean) -> Unit)? = null

    var arrangement: PageArrangement = PageArrangement.Default
        set(value) {
            field = value
            updateContentDescription()
            invalidate()
        }

    var enabledPages: Set<LauncherPage> = LauncherPage.entries.toSet()
        set(value) {
            field = value
            updateContentDescription()
            invalidate()
        }

    private var pageLabels = LauncherPage.entries.associateWith { it.name }
    private var homeLabel = "Home"
    private var fixedLabel = "Fixed"
    private var enabledLabel = "Enabled"
    private var disabledLabel = "Disabled"
    private var pressedPage: LauncherPage? = null
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var draggedPage: LauncherPage? = null
    private var dragX = 0f
    private var dragY = 0f
    private var dragTarget: PagePosition? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        isClickable = true
        isFocusable = true
    }

    fun setLabels(
        pageLabels: Map<LauncherPage, String>,
        homeLabel: String,
        fixedLabel: String,
        enabledLabel: String,
        disabledLabel: String,
    ) {
        this.pageLabels = pageLabels
        this.homeLabel = homeLabel
        this.fixedLabel = fixedLabel
        this.enabledLabel = enabledLabel
        this.disabledLabel = disabledLabel
        updateContentDescription()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = 280f.dp.toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellSize = min(width / 3.35f, height / 3.35f)
        val horizontalOffset = min(width * 0.31f, cellSize * 1.22f)
        val verticalOffset = min(height * 0.31f, cellSize * 1.22f)
        val centerX = width / 2f
        val centerY = height / 2f
        val cornerRadius = 12f.dp

        PagePosition.entries.forEach { position ->
            val (x, y) = centerFor(position, centerX, centerY, horizontalOffset, verticalOffset)
            val rect = cellRect(x, y, cellSize)
            val page = arrangement.pageAt(position)
            val isTarget = position == dragTarget
            fillPaint.color = if (isTarget) {
                ContextCompat.getColor(context, R.color.settings_option_divider)
            } else {
                android.graphics.Color.TRANSPARENT
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
            outlinePaint.color = ContextCompat.getColor(
                context,
                when {
                    isTarget -> R.color.launcher_text_secondary
                    page == null -> R.color.settings_option_divider
                    page in enabledPages -> R.color.launcher_text_secondary
                    else -> R.color.settings_option_divider
                },
            )
            outlinePaint.pathEffect = if (page == null) {
                DashPathEffect(floatArrayOf(7f.dp, 6f.dp), 0f)
            } else {
                null
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, outlinePaint)
            if (page != null && page != draggedPage) {
                drawPageLabel(canvas, page, x, y)
            }
        }

        val homeRect = cellRect(centerX, centerY, cellSize)
        fillPaint.color = ContextCompat.getColor(context, R.color.settings_option_divider)
        canvas.drawRoundRect(homeRect, cornerRadius, cornerRadius, fillPaint)
        outlinePaint.pathEffect = null
        outlinePaint.color = ContextCompat.getColor(context, R.color.launcher_text)
        canvas.drawRoundRect(homeRect, cornerRadius, cornerRadius, outlinePaint)
        textPaint.color = ContextCompat.getColor(context, R.color.launcher_text)
        textPaint.textSize = 15f.sp
        secondaryTextPaint.color = ContextCompat.getColor(context, R.color.launcher_text_secondary)
        secondaryTextPaint.textSize = 11f.sp
        canvas.drawText(homeLabel, centerX, centerY - 2f.dp, textPaint)
        canvas.drawText(fixedLabel, centerX, centerY + 17f.dp, secondaryTextPaint)

        draggedPage?.let { page ->
            val rect = cellRect(dragX, dragY, cellSize)
            fillPaint.color = ContextCompat.getColor(context, R.color.launcher_background)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
            outlinePaint.color = ContextCompat.getColor(context, R.color.launcher_text)
            outlinePaint.pathEffect = null
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, outlinePaint)
            drawPageLabel(canvas, page, dragX, dragY)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cellSize = min(width / 3.35f, height / 3.35f)
        val horizontalOffset = min(width * 0.31f, cellSize * 1.22f)
        val verticalOffset = min(height * 0.31f, cellSize * 1.22f)
        val centerX = width / 2f
        val centerY = height / 2f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val position = positionAt(
                    event.x,
                    event.y,
                    centerX,
                    centerY,
                    horizontalOffset,
                    verticalOffset,
                    cellSize,
                ) ?: return false
                val page = arrangement.pageAt(position) ?: return false
                pressedPage = page
                downX = event.x
                downY = event.y
                dragX = event.x
                dragY = event.y
                dragTarget = position
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val page = pressedPage ?: return false
                if (!isDragging && hypot(event.x - downX, event.y - downY) >= touchSlop) {
                    isDragging = true
                    draggedPage = page
                }
                dragX = event.x
                dragY = event.y
                dragTarget = positionAt(
                    event.x,
                    event.y,
                    centerX,
                    centerY,
                    horizontalOffset,
                    verticalOffset,
                    cellSize,
                )
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val page = pressedPage ?: return false
                val target = positionAt(
                    event.x,
                    event.y,
                    centerX,
                    centerY,
                    horizontalOffset,
                    verticalOffset,
                    cellSize,
                )
                if (isDragging && target != null) {
                    val updated = arrangement.move(page, target)
                    if (updated != arrangement) {
                        arrangement = updated
                        onArrangementChanged?.invoke(updated)
                    }
                } else if (!isDragging && target != null && arrangement.pageAt(target) == page) {
                    val isEnabled = page !in enabledPages
                    enabledPages = if (isEnabled) enabledPages + page else enabledPages - page
                    onPageEnabledChanged?.invoke(page, isEnabled)
                    performClick()
                }
                finishInteraction()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                finishInteraction()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishInteraction() {
        pressedPage = null
        isDragging = false
        draggedPage = null
        dragTarget = null
        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    private fun drawPageLabel(canvas: Canvas, page: LauncherPage, x: Float, y: Float) {
        val isEnabled = page in enabledPages
        textPaint.color = ContextCompat.getColor(
            context,
            if (isEnabled) R.color.launcher_text else R.color.launcher_text_secondary,
        )
        textPaint.textSize = 14f.sp
        val labelCenterY = y - 7f.dp
        canvas.drawText(
            pageLabels.getValue(page),
            x,
            labelCenterY - (textPaint.ascent() + textPaint.descent()) / 2f,
            textPaint,
        )
        secondaryTextPaint.color = ContextCompat.getColor(context, R.color.launcher_text_secondary)
        secondaryTextPaint.textSize = 10.5f.sp
        val stateCenterY = y + 13f.dp
        canvas.drawText(
            if (isEnabled) enabledLabel else disabledLabel,
            x,
            stateCenterY - (secondaryTextPaint.ascent() + secondaryTextPaint.descent()) / 2f,
            secondaryTextPaint,
        )
    }

    private fun positionAt(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        horizontalOffset: Float,
        verticalOffset: Float,
        cellSize: Float,
    ): PagePosition? {
        return PagePosition.entries.firstOrNull { position ->
            val (slotX, slotY) = centerFor(position, centerX, centerY, horizontalOffset, verticalOffset)
            cellRect(slotX, slotY, cellSize).contains(x, y)
        }
    }

    private fun centerFor(
        position: PagePosition,
        centerX: Float,
        centerY: Float,
        horizontalOffset: Float,
        verticalOffset: Float,
    ): Pair<Float, Float> {
        return when (position) {
            PagePosition.Up -> centerX to centerY - verticalOffset
            PagePosition.Left -> centerX - horizontalOffset to centerY
            PagePosition.Right -> centerX + horizontalOffset to centerY
            PagePosition.Down -> centerX to centerY + verticalOffset
        }
    }

    private fun cellRect(centerX: Float, centerY: Float, size: Float): RectF {
        val half = size / 2f
        return RectF(centerX - half, centerY - half, centerX + half, centerY + half)
    }

    private fun updateContentDescription() {
        contentDescription = LauncherPage.entries.joinToString(
            prefix = "$homeLabel, $fixedLabel. ",
            separator = ". ",
        ) { page ->
            val state = if (page in enabledPages) enabledLabel else disabledLabel
            "${pageLabels.getValue(page)} ${arrangement.positionOf(page).name.lowercase()}, $state"
        }
    }

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density

    private val Float.sp: Float
        get() = this * resources.displayMetrics.scaledDensity
}
