package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import com.example.textlauncher.domain.AppShortcut

class ShortcutRepository(context: Context) : ShortcutStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun loadShortcuts(): List<AppShortcut> {
        val stored = preferences.getString(KEY_SHORTCUTS, null) ?: return emptyList()
        return ShortcutJsonCodec.decodeOrEmpty(stored)
    }

    override fun saveShortcuts(shortcuts: List<AppShortcut>) {
        preferences.edit {
            putString(KEY_SHORTCUTS, ShortcutJsonCodec.encode(shortcuts))
        }
    }

    fun deleteShortcutsForPackage(packageName: String): List<AppShortcut> {
        val shortcuts = loadShortcuts()
        val updated = shortcuts.filterNot { it.packageName == packageName }
        if (updated.size != shortcuts.size) {
            saveShortcuts(updated)
        }
        return updated
    }

    private companion object {
        const val PREFERENCES_NAME = "shortcuts"
        const val KEY_SHORTCUTS = "shortcuts"
    }
}
