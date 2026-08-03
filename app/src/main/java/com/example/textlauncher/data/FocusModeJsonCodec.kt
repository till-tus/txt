package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.FocusMode
import com.example.textlauncher.domain.FocusSchedule
import org.json.JSONArray
import org.json.JSONObject

internal object FocusModeJsonCodec {
    fun decodeOrEmpty(stored: String): List<FocusMode> {
        return runCatching {
            val items = JSONArray(stored)
            buildList {
                for (index in 0 until items.length()) {
                    decodeMode(items.optJSONObject(index))?.let(::add)
                }
            }
        }.getOrElse { emptyList() }
    }

    fun encode(focusModes: List<FocusMode>): String {
        return JSONArray().apply {
            focusModes.forEach { mode ->
                put(
                    JSONObject()
                        .put(FIELD_ID, mode.id)
                        .put(FIELD_NAME, mode.name)
                        .put(FIELD_BLOCKED_APPS, JSONArray(mode.blockedAppPackageNames.toList()))
                        .put(FIELD_BUDGETS, encodeBudgets(mode.appBudgetMinutesByPackage))
                        .put(FIELD_SHORTCUTS, encodeShortcuts(mode.shortcuts))
                        .put(FIELD_SCHEDULE, encodeSchedule(mode.schedule)),
                )
            }
        }.toString()
    }

    private fun decodeMode(item: JSONObject?): FocusMode? {
        item ?: return null
        val id = item.optString(FIELD_ID).takeIf(String::isNotBlank) ?: return null
        val name = item.optString(FIELD_NAME).trim().takeIf(String::isNotEmpty) ?: return null
        return FocusMode(
            id = id,
            name = name,
            blockedAppPackageNames = item.optJSONArray(FIELD_BLOCKED_APPS).toStringSet(),
            appBudgetMinutesByPackage = decodeBudgets(item.optJSONArray(FIELD_BUDGETS)),
            shortcuts = decodeShortcuts(item.optJSONArray(FIELD_SHORTCUTS)),
            schedule = decodeSchedule(item.optJSONObject(FIELD_SCHEDULE)),
        )
    }

    private fun encodeBudgets(budgets: Map<String, Int>): JSONArray {
        return JSONArray().apply {
            budgets.forEach { (packageName, minutes) ->
                put(JSONObject().put(FIELD_PACKAGE, packageName).put(FIELD_MINUTES, minutes))
            }
        }
    }

    private fun decodeBudgets(items: JSONArray?): Map<String, Int> {
        items ?: return emptyMap()
        return buildMap {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val packageName = item.optString(FIELD_PACKAGE).takeIf(String::isNotBlank) ?: continue
                val minutes = item.optInt(FIELD_MINUTES, 0).takeIf { it > 0 } ?: continue
                put(packageName, minutes)
            }
        }
    }

    private fun encodeShortcuts(shortcuts: List<AppShortcut>): JSONArray {
        return JSONArray().apply {
            shortcuts.forEach { shortcut ->
                put(
                    JSONObject()
                        .put(FIELD_LABEL, shortcut.label)
                        .put(FIELD_PACKAGE, shortcut.packageName)
                        .put(FIELD_ACTIVITY, shortcut.activityName),
                )
            }
        }
    }

    private fun decodeShortcuts(items: JSONArray?): List<AppShortcut> {
        items ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val packageName = item.optString(FIELD_PACKAGE).takeIf(String::isNotBlank) ?: continue
                val activityName = item.optString(FIELD_ACTIVITY).takeIf(String::isNotBlank) ?: continue
                add(
                    AppShortcut(
                        label = item.optString(FIELD_LABEL, packageName),
                        packageName = packageName,
                        activityName = activityName,
                    ),
                )
            }
        }
    }

    private fun encodeSchedule(schedule: FocusSchedule): JSONObject {
        return JSONObject()
            .put(FIELD_ENABLED, schedule.enabled)
            .put(FIELD_DAYS, JSONArray(schedule.daysOfWeek.toList()))
            .put(FIELD_START_MINUTE, schedule.startMinuteOfDay)
            .put(FIELD_END_MINUTE, schedule.endMinuteOfDay)
    }

    private fun decodeSchedule(item: JSONObject?): FocusSchedule {
        item ?: return FocusSchedule()
        return FocusSchedule(
            enabled = item.optBoolean(FIELD_ENABLED, false),
            daysOfWeek = item.optJSONArray(FIELD_DAYS).toIntSet().filter { it in 1..7 }.toSet(),
            startMinuteOfDay = item.optInt(FIELD_START_MINUTE, 9 * 60).coerceIn(0, 24 * 60 - 1),
            endMinuteOfDay = item.optInt(FIELD_END_MINUTE, 17 * 60).coerceIn(0, 24 * 60 - 1),
        )
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        this ?: return emptySet()
        return buildSet {
            for (index in 0 until length()) {
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private fun JSONArray?.toIntSet(): Set<Int> {
        this ?: return emptySet()
        return buildSet {
            for (index in 0 until length()) add(optInt(index))
        }
    }

    private const val FIELD_ID = "id"
    private const val FIELD_NAME = "name"
    private const val FIELD_BLOCKED_APPS = "blockedApps"
    private const val FIELD_BUDGETS = "budgets"
    private const val FIELD_SHORTCUTS = "shortcuts"
    private const val FIELD_SCHEDULE = "schedule"
    private const val FIELD_ENABLED = "enabled"
    private const val FIELD_DAYS = "days"
    private const val FIELD_START_MINUTE = "startMinute"
    private const val FIELD_END_MINUTE = "endMinute"
    private const val FIELD_LABEL = "label"
    private const val FIELD_PACKAGE = "packageName"
    private const val FIELD_ACTIVITY = "activityName"
    private const val FIELD_MINUTES = "minutes"
}
