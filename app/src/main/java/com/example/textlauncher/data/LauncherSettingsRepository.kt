package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.LauncherSettings
import com.example.textlauncher.domain.ShortcutTextAlignment

class LauncherSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): LauncherSettings {
        return LauncherSettings(
            showDate = preferences.getBoolean(KEY_SHOW_DATE, true),
            clockDisplayMode = preferences.getString(KEY_CLOCK_DISPLAY_MODE, ClockDisplayMode.Analog.name)
                ?.let(::runCatchingClockMode)
                ?: ClockDisplayMode.Analog,
            showQuickAccess = preferences.getBoolean(KEY_SHOW_QUICK_ACCESS, false),
            wallpaperDimPercent = preferences.getInt(KEY_WALLPAPER_DIM_PERCENT, DEFAULT_WALLPAPER_DIM_PERCENT)
                .coerceIn(0, 100),
            shortcutTextAlignment = preferences.getString(KEY_SHORTCUT_TEXT_ALIGNMENT, ShortcutTextAlignment.Left.name)
                ?.let(::runCatchingShortcutTextAlignment)
                ?: ShortcutTextAlignment.Left,
            maxShortcuts = preferences.getInt(KEY_MAX_SHORTCUTS, 5).coerceIn(3, 7),
            openAppListKeyboardAutomatically = preferences.getBoolean(
                KEY_OPEN_APP_LIST_KEYBOARD_AUTOMATICALLY,
                true,
            ),
            openScreenTimeGesture = loadGesture(
                key = KEY_OPEN_SCREEN_TIME_GESTURE,
                defaultGesture = if (preferences.getBoolean(KEY_SHOW_SCREEN_TIME_PAGE, true)) {
                    LauncherGesture.TwoFingerSwipeDown
                } else {
                    LauncherGesture.None
                },
            ),
            lockScreenGesture = loadGesture(KEY_LOCK_SCREEN_GESTURE, LauncherGesture.DoubleTap),
            showNotesPage = preferences.getBoolean(KEY_SHOW_NOTES_PAGE, true),
            showCalendarPage = preferences.getBoolean(KEY_SHOW_CALENDAR_PAGE, true),
            showTodayPage = preferences.getBoolean(KEY_SHOW_TODAY_PAGE, true),
            selectedCalendarIds = preferences.getStringSet(KEY_SELECTED_CALENDAR_IDS, emptySet())
                .orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .toSet(),
            blockedAppPackageNames = preferences.getStringSet(KEY_BLOCKED_APP_PACKAGE_NAMES, emptySet()).orEmpty(),
            appBudgetMinutesByPackage = preferences.getStringSet(KEY_APP_BUDGETS, emptySet())
                .orEmpty()
                .mapNotNull(::parseAppBudget)
                .toMap(),
            hasRequestedCalendarPermission = preferences.getBoolean(KEY_HAS_REQUESTED_CALENDAR_PERMISSION, false),
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        preferences.edit {
            putBoolean(KEY_SHOW_DATE, settings.showDate)
            putString(KEY_CLOCK_DISPLAY_MODE, settings.clockDisplayMode.name)
            putBoolean(KEY_SHOW_QUICK_ACCESS, settings.showQuickAccess)
            putInt(KEY_WALLPAPER_DIM_PERCENT, settings.wallpaperDimPercent.coerceIn(0, 100))
            putString(KEY_SHORTCUT_TEXT_ALIGNMENT, settings.shortcutTextAlignment.name)
            putInt(KEY_MAX_SHORTCUTS, settings.maxShortcuts.coerceIn(3, 7))
            putBoolean(
                KEY_OPEN_APP_LIST_KEYBOARD_AUTOMATICALLY,
                settings.openAppListKeyboardAutomatically,
            )
            putBoolean(KEY_SHOW_SCREEN_TIME_PAGE, settings.showScreenTimePage)
            putString(KEY_OPEN_SCREEN_TIME_GESTURE, settings.openScreenTimeGesture.name)
            putString(KEY_LOCK_SCREEN_GESTURE, settings.lockScreenGesture.name)
            putBoolean(KEY_SHOW_NOTES_PAGE, settings.showNotesPage)
            putBoolean(KEY_SHOW_CALENDAR_PAGE, settings.showCalendarPage)
            putBoolean(KEY_SHOW_TODAY_PAGE, settings.showTodayPage)
            putStringSet(KEY_SELECTED_CALENDAR_IDS, settings.selectedCalendarIds.map { it.toString() }.toSet())
            putStringSet(KEY_BLOCKED_APP_PACKAGE_NAMES, settings.blockedAppPackageNames)
            putStringSet(
                KEY_APP_BUDGETS,
                settings.appBudgetMinutesByPackage.map { (packageName, minutes) -> "$packageName|$minutes" }.toSet(),
            )
            putBoolean(KEY_HAS_REQUESTED_CALENDAR_PERMISSION, settings.hasRequestedCalendarPermission)
        }
    }

    private fun runCatchingClockMode(value: String): ClockDisplayMode? {
        return runCatching { ClockDisplayMode.valueOf(value) }.getOrNull()
    }

    private fun runCatchingShortcutTextAlignment(value: String): ShortcutTextAlignment? {
        return runCatching { ShortcutTextAlignment.valueOf(value) }.getOrNull()
    }

    private fun loadGesture(key: String, defaultGesture: LauncherGesture): LauncherGesture {
        return preferences.getString(key, defaultGesture.name)
            ?.let { value -> runCatching { LauncherGesture.valueOf(value) }.getOrNull() }
            ?: defaultGesture
    }

    private fun parseAppBudget(value: String): Pair<String, Int>? {
        val separatorIndex = value.lastIndexOf('|')
        if (separatorIndex <= 0 || separatorIndex == value.lastIndex) return null

        val packageName = value.substring(0, separatorIndex)
        val minutes = value.substring(separatorIndex + 1).toIntOrNull() ?: return null
        return packageName to minutes
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_settings"
        const val KEY_SHOW_DATE = "showDate"
        const val KEY_CLOCK_DISPLAY_MODE = "clockDisplayMode"
        const val KEY_SHOW_QUICK_ACCESS = "showQuickAccess"
        const val KEY_WALLPAPER_DIM_PERCENT = "wallpaperDimPercent"
        const val KEY_SHORTCUT_TEXT_ALIGNMENT = "shortcutTextAlignment"
        const val KEY_MAX_SHORTCUTS = "maxShortcuts"
        const val KEY_OPEN_APP_LIST_KEYBOARD_AUTOMATICALLY = "openAppListKeyboardAutomatically"
        const val KEY_SHOW_SCREEN_TIME_PAGE = "showScreenTimePage"
        const val KEY_OPEN_SCREEN_TIME_GESTURE = "openScreenTimeGesture"
        const val KEY_LOCK_SCREEN_GESTURE = "lockScreenGesture"
        const val KEY_SHOW_NOTES_PAGE = "showNotesPage"
        const val KEY_SHOW_CALENDAR_PAGE = "showCalendarPage"
        const val KEY_SHOW_TODAY_PAGE = "showTodayPage"
        const val KEY_SELECTED_CALENDAR_IDS = "selectedCalendarIds"
        const val KEY_BLOCKED_APP_PACKAGE_NAMES = "blockedAppPackageNames"
        const val KEY_APP_BUDGETS = "appBudgets"
        const val KEY_HAS_REQUESTED_CALENDAR_PERMISSION = "hasRequestedCalendarPermission"
        const val DEFAULT_WALLPAPER_DIM_PERCENT = 70
    }
}
