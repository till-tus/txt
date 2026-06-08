package com.example.textlauncher.domain

data class LauncherSettings(
    val showDate: Boolean = true,
    val clockDisplayMode: ClockDisplayMode = ClockDisplayMode.Analog,
    val showQuickAccess: Boolean = false,
    val wallpaperDimPercent: Int = 70,
    val shortcutTextAlignment: ShortcutTextAlignment = ShortcutTextAlignment.Left,
    val maxShortcuts: Int = 5,
    val openScreenTimeGesture: LauncherGesture = LauncherGesture.TwoFingerSwipeDown,
    val lockScreenGesture: LauncherGesture = LauncherGesture.DoubleTap,
    val showNotesPage: Boolean = true,
    val showCalendarPage: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val blockedAppPackageNames: Set<String> = emptySet(),
    val appBudgetMinutesByPackage: Map<String, Int> = emptyMap(),
    val hasRequestedCalendarPermission: Boolean = false,
) {
    val showScreenTimePage: Boolean
        get() = openScreenTimeGesture != LauncherGesture.None
}
