package com.example.textlauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.LauncherSettings
import com.example.textlauncher.domain.PageArrangement
import com.example.textlauncher.domain.PagePosition
import com.example.textlauncher.domain.QuickAccessIcon
import com.example.textlauncher.domain.QuickAccessPosition
import com.example.textlauncher.domain.QuickAccessTarget
import com.example.textlauncher.domain.ShortcutTextAlignment

class LauncherSettingsRepository(context: Context) : LauncherSettingsStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun loadSettings(): LauncherSettings {
        val pageArrangement = loadPageArrangement()
        return LauncherSettings(
            showDate = preferences.getBoolean(KEY_SHOW_DATE, true),
            clockDisplayMode = preferences.getString(KEY_CLOCK_DISPLAY_MODE, ClockDisplayMode.Analog.name)
                ?.let(::runCatchingClockMode)
                ?: ClockDisplayMode.Analog,
            leftQuickAccess = loadQuickAccessTarget(QUICK_ACCESS_LEFT_PREFIX),
            rightQuickAccess = loadQuickAccessTarget(QUICK_ACCESS_RIGHT_PREFIX),
            quickAccessPosition = preferences.getString(
                KEY_QUICK_ACCESS_POSITION,
                QuickAccessPosition.BothCenter.name,
            )?.let { value -> runCatching { QuickAccessPosition.valueOf(value) }.getOrNull() }
                ?: QuickAccessPosition.BothCenter,
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
            universalCommandPaletteEnabled = preferences.getBoolean(KEY_UNIVERSAL_COMMAND_PALETTE_ENABLED, false),
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
            pageArrangement = pageArrangement,
            selectedCalendarIds = preferences.getStringSet(KEY_SELECTED_CALENDAR_IDS, emptySet())
                .orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .toSet(),
            blockedAppPackageNames = preferences.getStringSet(KEY_BLOCKED_APP_PACKAGE_NAMES, emptySet()).orEmpty(),
            appBudgetMinutesByPackage = preferences.getStringSet(KEY_APP_BUDGETS, emptySet())
                .orEmpty()
                .mapNotNull(::parseAppBudget)
                .toMap(),
            excludedScreenTimePackageNames = preferences.getStringSet(
                KEY_EXCLUDED_SCREEN_TIME_PACKAGE_NAMES,
                emptySet(),
            ).orEmpty(),
            hasRequestedCalendarPermission = preferences.getBoolean(KEY_HAS_REQUESTED_CALENDAR_PERMISSION, false),
        )
    }

    override fun saveSettings(settings: LauncherSettings) {
        preferences.edit {
            putBoolean(KEY_SHOW_DATE, settings.showDate)
            putString(KEY_CLOCK_DISPLAY_MODE, settings.clockDisplayMode.name)
            putQuickAccessTarget(QUICK_ACCESS_LEFT_PREFIX, settings.leftQuickAccess)
            putQuickAccessTarget(QUICK_ACCESS_RIGHT_PREFIX, settings.rightQuickAccess)
            putString(KEY_QUICK_ACCESS_POSITION, settings.quickAccessPosition.name)
            putInt(KEY_WALLPAPER_DIM_PERCENT, settings.wallpaperDimPercent.coerceIn(0, 100))
            putString(KEY_SHORTCUT_TEXT_ALIGNMENT, settings.shortcutTextAlignment.name)
            putInt(KEY_MAX_SHORTCUTS, settings.maxShortcuts.coerceIn(3, 7))
            putBoolean(
                KEY_OPEN_APP_LIST_KEYBOARD_AUTOMATICALLY,
                settings.openAppListKeyboardAutomatically,
            )
            putBoolean(KEY_UNIVERSAL_COMMAND_PALETTE_ENABLED, settings.universalCommandPaletteEnabled)
            putBoolean(KEY_SHOW_SCREEN_TIME_PAGE, settings.showScreenTimePage)
            putString(KEY_OPEN_SCREEN_TIME_GESTURE, settings.openScreenTimeGesture.name)
            putString(KEY_LOCK_SCREEN_GESTURE, settings.lockScreenGesture.name)
            putBoolean(KEY_SHOW_NOTES_PAGE, settings.showNotesPage)
            putBoolean(KEY_SHOW_CALENDAR_PAGE, settings.showCalendarPage)
            putBoolean(KEY_SHOW_TODAY_PAGE, settings.showTodayPage)
            putString(KEY_NOTES_PAGE_POSITION, settings.pageArrangement.notesPosition.name)
            putString(KEY_TODAY_PAGE_POSITION, settings.pageArrangement.todayPosition.name)
            putString(KEY_CALENDAR_PAGE_POSITION, settings.pageArrangement.calendarPosition.name)
            putStringSet(KEY_SELECTED_CALENDAR_IDS, settings.selectedCalendarIds.map { it.toString() }.toSet())
            putStringSet(KEY_BLOCKED_APP_PACKAGE_NAMES, settings.blockedAppPackageNames)
            putStringSet(
                KEY_APP_BUDGETS,
                settings.appBudgetMinutesByPackage.map { (packageName, minutes) -> "$packageName|$minutes" }.toSet(),
            )
            putStringSet(KEY_EXCLUDED_SCREEN_TIME_PACKAGE_NAMES, settings.excludedScreenTimePackageNames)
            putBoolean(KEY_HAS_REQUESTED_CALENDAR_PERMISSION, settings.hasRequestedCalendarPermission)
        }
    }

    fun removePackageReferences(packageName: String) {
        val current = loadSettings()
        val updated = current.copy(
            leftQuickAccess = current.leftQuickAccess?.takeUnless { it.packageName == packageName },
            rightQuickAccess = current.rightQuickAccess?.takeUnless { it.packageName == packageName },
            blockedAppPackageNames = current.blockedAppPackageNames - packageName,
            appBudgetMinutesByPackage = current.appBudgetMinutesByPackage - packageName,
            excludedScreenTimePackageNames = current.excludedScreenTimePackageNames - packageName,
        )
        if (updated != current) {
            saveSettings(updated)
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

    private fun loadPagePosition(key: String): PagePosition? {
        return preferences.getString(key, null)
            ?.let { value -> runCatching { PagePosition.valueOf(value) }.getOrNull() }
    }

    private fun loadPageArrangement(): PageArrangement {
        val notesPosition = loadPagePosition(KEY_NOTES_PAGE_POSITION)
        val todayPosition = loadPagePosition(KEY_TODAY_PAGE_POSITION)
        val calendarPosition = loadPagePosition(KEY_CALENDAR_PAGE_POSITION)
        val arrangement = PageArrangement.validatedOrDefault(
            notesPosition = notesPosition,
            todayPosition = todayPosition,
            calendarPosition = calendarPosition,
        )
        val hasStoredArrangement = listOf(
            KEY_NOTES_PAGE_POSITION,
            KEY_TODAY_PAGE_POSITION,
            KEY_CALENDAR_PAGE_POSITION,
        ).any(preferences::contains)
        val needsMigration = notesPosition != arrangement.notesPosition ||
            todayPosition != arrangement.todayPosition ||
            calendarPosition != arrangement.calendarPosition
        if (hasStoredArrangement && needsMigration) {
            preferences.edit {
                putString(KEY_NOTES_PAGE_POSITION, arrangement.notesPosition.name)
                putString(KEY_TODAY_PAGE_POSITION, arrangement.todayPosition.name)
                putString(KEY_CALENDAR_PAGE_POSITION, arrangement.calendarPosition.name)
            }
        }
        return arrangement
    }

    private fun loadQuickAccessTarget(prefix: String): QuickAccessTarget? {
        val packageName = preferences.getString("${prefix}Package", null)?.takeIf(String::isNotBlank)
            ?: return null
        val activityName = preferences.getString("${prefix}Activity", null)?.takeIf(String::isNotBlank)
            ?: return null
        val label = preferences.getString("${prefix}Label", null)?.takeIf(String::isNotBlank)
            ?: packageName
        val icon = preferences.getString("${prefix}Icon", null)
            ?.let { value -> runCatching { QuickAccessIcon.valueOf(value) }.getOrNull() }
            ?: QuickAccessIcon.Camera
        return QuickAccessTarget(
            label = label,
            packageName = packageName,
            activityName = activityName,
            icon = icon,
        )
    }

    private fun SharedPreferences.Editor.putQuickAccessTarget(prefix: String, target: QuickAccessTarget?) {
        if (target == null) {
            remove("${prefix}Label")
            remove("${prefix}Package")
            remove("${prefix}Activity")
            remove("${prefix}Icon")
            return
        }
        putString("${prefix}Label", target.label)
        putString("${prefix}Package", target.packageName)
        putString("${prefix}Activity", target.activityName)
        putString("${prefix}Icon", target.icon.name)
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
        const val QUICK_ACCESS_LEFT_PREFIX = "quickAccessLeft"
        const val QUICK_ACCESS_RIGHT_PREFIX = "quickAccessRight"
        const val KEY_QUICK_ACCESS_POSITION = "quickAccessPosition"
        const val KEY_WALLPAPER_DIM_PERCENT = "wallpaperDimPercent"
        const val KEY_SHORTCUT_TEXT_ALIGNMENT = "shortcutTextAlignment"
        const val KEY_MAX_SHORTCUTS = "maxShortcuts"
        const val KEY_OPEN_APP_LIST_KEYBOARD_AUTOMATICALLY = "openAppListKeyboardAutomatically"
        const val KEY_UNIVERSAL_COMMAND_PALETTE_ENABLED = "universalCommandPaletteEnabled"
        const val KEY_SHOW_SCREEN_TIME_PAGE = "showScreenTimePage"
        const val KEY_OPEN_SCREEN_TIME_GESTURE = "openScreenTimeGesture"
        const val KEY_LOCK_SCREEN_GESTURE = "lockScreenGesture"
        const val KEY_SHOW_NOTES_PAGE = "showNotesPage"
        const val KEY_SHOW_CALENDAR_PAGE = "showCalendarPage"
        const val KEY_SHOW_TODAY_PAGE = "showTodayPage"
        const val KEY_NOTES_PAGE_POSITION = "notesPagePosition"
        const val KEY_TODAY_PAGE_POSITION = "todayPagePosition"
        const val KEY_CALENDAR_PAGE_POSITION = "calendarPagePosition"
        const val KEY_SELECTED_CALENDAR_IDS = "selectedCalendarIds"
        const val KEY_BLOCKED_APP_PACKAGE_NAMES = "blockedAppPackageNames"
        const val KEY_APP_BUDGETS = "appBudgets"
        const val KEY_EXCLUDED_SCREEN_TIME_PACKAGE_NAMES = "excludedScreenTimePackageNames"
        const val KEY_HAS_REQUESTED_CALENDAR_PERMISSION = "hasRequestedCalendarPermission"
        const val DEFAULT_WALLPAPER_DIM_PERCENT = 70
    }
}
