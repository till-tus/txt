package com.example.textlauncher.ui

import com.example.textlauncher.domain.LauncherPage
import com.example.textlauncher.domain.PagePosition

internal enum class PageSwipeDirection {
    Left,
    Right,
    Up,
    Down,
}

internal data class PageSwipeTarget(
    val page: LauncherPage,
    val position: PagePosition,
    val isReturningHome: Boolean,
) {
    val isVertical: Boolean
        get() = position == PagePosition.Down

    val expectedGestureDirection: PageSwipeDirection
        get() = if (isReturningHome) {
            position.toSwipeDirection()
        } else {
            position.toSwipeDirection().opposite()
        }
}

internal fun PagePosition.toSwipeDirection(): PageSwipeDirection {
    return when (this) {
        PagePosition.Left -> PageSwipeDirection.Left
        PagePosition.Right -> PageSwipeDirection.Right
        PagePosition.Down -> PageSwipeDirection.Down
    }
}

internal fun PageSwipeDirection.opposite(): PageSwipeDirection {
    return when (this) {
        PageSwipeDirection.Left -> PageSwipeDirection.Right
        PageSwipeDirection.Right -> PageSwipeDirection.Left
        PageSwipeDirection.Up -> PageSwipeDirection.Down
        PageSwipeDirection.Down -> PageSwipeDirection.Up
    }
}
