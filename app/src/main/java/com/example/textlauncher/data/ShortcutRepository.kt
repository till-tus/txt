package com.example.textlauncher.data

import android.content.Context
import com.example.textlauncher.domain.AppShortcut
import org.json.JSONArray
import org.json.JSONObject

class ShortcutRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadShortcuts(): List<AppShortcut> {
        val stored = preferences.getString(KEY_SHORTCUTS, null) ?: return emptyList()
        val items = JSONArray(stored)
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AppShortcut(
                        label = item.getString(KEY_LABEL),
                        packageName = item.getString(KEY_PACKAGE),
                        activityName = item.getString(KEY_ACTIVITY),
                    ),
                )
            }
        }
    }

    fun saveShortcuts(shortcuts: List<AppShortcut>) {
        val items = JSONArray()
        shortcuts.forEach { shortcut ->
            items.put(
                JSONObject()
                    .put(KEY_LABEL, shortcut.label)
                    .put(KEY_PACKAGE, shortcut.packageName)
                    .put(KEY_ACTIVITY, shortcut.activityName),
            )
        }
        preferences.edit().putString(KEY_SHORTCUTS, items.toString()).apply()
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
        const val KEY_LABEL = "label"
        const val KEY_PACKAGE = "packageName"
        const val KEY_ACTIVITY = "activityName"
    }
}
