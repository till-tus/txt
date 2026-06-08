package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.textlauncher.R

internal data class ContextMenuAction(
    val label: String,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit,
)

internal class ActionContextMenu(
    private val context: Context,
) {
    fun show(anchor: View, actions: List<ContextMenuAction>) {
        val menuView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(context.getColor(R.color.launcher_background))
                setStroke(context.dp(1), context.getColor(R.color.launcher_text))
            }
        }

        val popup = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 0f
        }

        actions.forEach { action ->
            val row = TextView(context).apply {
                text = action.label
                setTextColor(
                    context.getColor(
                        if (action.isEnabled) {
                            R.color.launcher_text
                        } else {
                            R.color.launcher_text_secondary
                        },
                    ),
                )
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                isEnabled = action.isEnabled
                alpha = if (action.isEnabled) 1f else DISABLED_ACTION_ALPHA
                minWidth = context.dp(MENU_WIDTH_DP)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    context.dp(MENU_HEIGHT_DP),
                )
                setPadding(context.dp(MENU_HORIZONTAL_PADDING_DP), 0, context.dp(MENU_HORIZONTAL_PADDING_DP), 0)
                setOnClickListener {
                    action.onClick()
                    popup.dismiss()
                }
            }
            menuView.addView(row)
        }
        showEdgeAware(popup, menuView, anchor)
    }

    private fun showEdgeAware(popup: PopupWindow, menuView: View, anchor: View) {
        val visibleFrame = Rect()
        anchor.rootView.getWindowVisibleDisplayFrame(visibleFrame)

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)

        menuView.measure(
            View.MeasureSpec.makeMeasureSpec(visibleFrame.width(), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(visibleFrame.height(), View.MeasureSpec.AT_MOST),
        )

        val margin = context.dp(SCREEN_EDGE_MARGIN_DP)
        val menuWidth = menuView.measuredWidth
        val menuHeight = menuView.measuredHeight
        val maxX = visibleFrame.right - menuWidth - margin
        val x = anchorLocation[0].coerceIn(visibleFrame.left + margin, maxX.coerceAtLeast(visibleFrame.left + margin))
        val belowY = anchorLocation[1] + anchor.height
        val aboveY = anchorLocation[1] - menuHeight
        val y = when {
            belowY + menuHeight + margin <= visibleFrame.bottom -> belowY
            aboveY >= visibleFrame.top + margin -> aboveY
            else -> {
                val maxY = visibleFrame.bottom - menuHeight - margin
                maxY.coerceAtLeast(visibleFrame.top + margin)
            }
        }
        popup.showAtLocation(anchor.rootView, Gravity.NO_GRAVITY, x, y)
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val MENU_WIDTH_DP = 112
        const val MENU_HEIGHT_DP = 48
        const val MENU_HORIZONTAL_PADDING_DP = 18
        const val SCREEN_EDGE_MARGIN_DP = 8
        const val DISABLED_ACTION_ALPHA = 0.34f
    }
}
