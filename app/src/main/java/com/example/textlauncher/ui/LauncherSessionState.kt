package com.example.textlauncher.ui

import java.io.Serializable

internal enum class LauncherSurface {
    Home,
    Notes,
    Calendar,
    Today,
    ScreenTime,
    Settings,
    AppPicker,
}

internal data class LauncherSessionState(
    val surface: LauncherSurface = LauncherSurface.Home,
    val isEditMode: Boolean = false,
    val isTodayEditMode: Boolean = false,
    val settingsPage: String = SettingsPage.Index.name,
    val isNoteTrashVisible: Boolean = false,
    val isNoteEditorVisible: Boolean = false,
    val editingNoteId: Long? = null,
    val noteDraft: String = "",
    val noteInputMode: String = NoteInputMode.Text.name,
    val appListMode: String = AppListMode.AddShortcut.name,
    val isScreenTimeExpanded: Boolean = false,
    val isScreenTimeIntentionsExpanded: Boolean = false,
    val isCalendarSelectionExpanded: Boolean = false,
    val isAppBlockingExpanded: Boolean = false,
    val isAppBudgetsExpanded: Boolean = false,
    val isScreenTimeExclusionsExpanded: Boolean = false,
) : Serializable {
    fun restoredSettingsPage(): SettingsPage {
        return enumValueOrDefault(settingsPage, SettingsPage.Index)
    }

    fun restoredNoteInputMode(): NoteInputMode {
        return enumValueOrDefault(noteInputMode, NoteInputMode.Text)
    }

    fun restoredAppListMode(): AppListMode {
        return enumValueOrDefault(appListMode, AppListMode.AddShortcut)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}
