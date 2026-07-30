package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.LauncherSettings

interface ShortcutStore {
    fun loadShortcuts(): List<AppShortcut>
    fun saveShortcuts(shortcuts: List<AppShortcut>)
}

interface LauncherSettingsStore {
    fun loadSettings(): LauncherSettings
    fun saveSettings(settings: LauncherSettings)
}

interface NoteStore {
    fun loadState(): NoteStoreState
    fun saveState(state: NoteStoreState)
}
