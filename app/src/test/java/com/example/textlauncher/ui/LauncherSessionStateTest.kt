package com.example.textlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSessionStateTest {
    @Test
    fun invalidEnumNames_restoreToSafeDefaults() {
        val state = LauncherSessionState(
            settingsPage = "removed-setting-page",
            noteInputMode = "removed-note-mode",
            appListMode = "removed-app-list-mode",
        )

        assertEquals(SettingsPage.Index, state.restoredSettingsPage())
        assertEquals(NoteInputMode.Text, state.restoredNoteInputMode())
        assertEquals(AppListMode.AddShortcut, state.restoredAppListMode())
    }
}
