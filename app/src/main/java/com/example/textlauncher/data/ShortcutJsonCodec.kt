package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import org.json.JSONArray
import org.json.JSONObject

internal object ShortcutJsonCodec {
    fun decodeOrEmpty(stored: String): List<AppShortcut> {
        return runCatching {
            val items = JSONArray(stored)
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    add(
                        AppShortcut(
                            label = item.getString(FIELD_LABEL),
                            packageName = item.getString(FIELD_PACKAGE),
                            activityName = item.getString(FIELD_ACTIVITY),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun encode(shortcuts: List<AppShortcut>): String {
        return JSONArray().apply {
            shortcuts.forEach { shortcut ->
                put(
                    JSONObject()
                        .put(FIELD_LABEL, shortcut.label)
                        .put(FIELD_PACKAGE, shortcut.packageName)
                        .put(FIELD_ACTIVITY, shortcut.activityName),
                )
            }
        }.toString()
    }

    private const val FIELD_LABEL = "label"
    private const val FIELD_PACKAGE = "packageName"
    private const val FIELD_ACTIVITY = "activityName"
}
