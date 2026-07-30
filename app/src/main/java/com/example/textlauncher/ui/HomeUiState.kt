package com.example.textlauncher.ui

import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.ShortcutTextAlignment
import com.example.textlauncher.domain.TrashedNote

data class HomeUiState(
    val shortcuts: List<AppShortcut> = emptyList(),
    val notes: List<QuickNote> = emptyList(),
    val trashedNotes: List<TrashedNote> = emptyList(),
    val showDate: Boolean = true,
    val clockDisplayMode: ClockDisplayMode = ClockDisplayMode.Analog,
    val showQuickAccess: Boolean = false,
    val wallpaperDimPercent: Int = 70,
    val shortcutTextAlignment: ShortcutTextAlignment = ShortcutTextAlignment.Left,
    val maxShortcuts: Int = 5,
    val openAppListKeyboardAutomatically: Boolean = true,
    val openScreenTimeGesture: LauncherGesture = LauncherGesture.TwoFingerSwipeDown,
    val lockScreenGesture: LauncherGesture = LauncherGesture.DoubleTap,
    val showNotesPage: Boolean = true,
    val showCalendarPage: Boolean = true,
    val showTodayPage: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val blockedAppPackageNames: Set<String> = emptySet(),
    val appBudgetMinutesByPackage: Map<String, Int> = emptyMap(),
    val hasRequestedCalendarPermission: Boolean = false,
) {
    val showScreenTimePage: Boolean
        get() = openScreenTimeGesture != LauncherGesture.None
}
