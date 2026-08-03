package com.example.textlauncher.domain

data class LauncherSettings(
    val showDate: Boolean = true,
    val clockDisplayMode: ClockDisplayMode = ClockDisplayMode.Analog,
    val leftQuickAccess: QuickAccessTarget? = null,
    val rightQuickAccess: QuickAccessTarget? = null,
    val quickAccessPosition: QuickAccessPosition = QuickAccessPosition.BothCenter,
    val wallpaperDimPercent: Int = 70,
    val shortcutTextAlignment: ShortcutTextAlignment = ShortcutTextAlignment.Left,
    val maxShortcuts: Int = 5,
    val openAppListKeyboardAutomatically: Boolean = true,
    val universalCommandPaletteEnabled: Boolean = false,
    val openScreenTimeGesture: LauncherGesture = LauncherGesture.TwoFingerSwipeDown,
    val lockScreenGesture: LauncherGesture = LauncherGesture.DoubleTap,
    val showNotesPage: Boolean = true,
    val showCalendarPage: Boolean = true,
    val showTodayPage: Boolean = true,
    val pageArrangement: PageArrangement = PageArrangement.Default,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val blockedAppPackageNames: Set<String> = emptySet(),
    val appBudgetMinutesByPackage: Map<String, Int> = emptyMap(),
    val excludedScreenTimePackageNames: Set<String> = emptySet(),
    val hasRequestedCalendarPermission: Boolean = false,
) {
    val showScreenTimePage: Boolean
        get() = openScreenTimeGesture != LauncherGesture.None
}
