package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppUsageIntentionRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun loadTodayIntendedMinutes(): Int {
        return loadTodayIntentionsByPackage().values.sum()
    }

    fun loadTodayIntentionsByPackage(): Map<String, Int> {
        resetIfNeeded()
        return preferences.getStringSet(KEY_INTENTIONS_BY_PACKAGE, emptySet())
            .orEmpty()
            .mapNotNull(::parseIntention)
            .toMap()
    }

    fun addTodayIntention(packageName: String, minutes: Int): Int {
        resetIfNeeded()
        val updatedIntentions = loadTodayIntentionsByPackage().toMutableMap()
        updatedIntentions[packageName] = (updatedIntentions[packageName] ?: 0) + minutes
        preferences.edit {
            putString(KEY_DATE, todayKey())
            putStringSet(
                KEY_INTENTIONS_BY_PACKAGE,
                updatedIntentions.map { (currentPackageName, currentMinutes) ->
                    "$currentPackageName|$currentMinutes"
                }.toSet(),
            )
        }
        return updatedIntentions.values.sum()
    }

    fun resetIntentions() {
        preferences.edit {
            putString(KEY_DATE, todayKey())
            putStringSet(KEY_INTENTIONS_BY_PACKAGE, emptySet())
        }
    }

    private fun resetIfNeeded() {
        val today = todayKey()
        if (preferences.getString(KEY_DATE, null) == today) return

        preferences.edit {
            putString(KEY_DATE, today)
            putStringSet(KEY_INTENTIONS_BY_PACKAGE, emptySet())
        }
    }

    private fun parseIntention(value: String): Pair<String, Int>? {
        val separatorIndex = value.lastIndexOf('|')
        if (separatorIndex <= 0 || separatorIndex == value.lastIndex) return null

        val packageName = value.substring(0, separatorIndex)
        val minutes = value.substring(separatorIndex + 1).toIntOrNull() ?: return null
        return packageName to minutes
    }

    private fun todayKey(): String {
        return dayFormat.format(Date())
    }

    private companion object {
        const val PREFERENCES_NAME = "app_usage_intentions"
        const val KEY_DATE = "date"
        const val KEY_INTENTIONS_BY_PACKAGE = "intentionsByPackage"
    }
}
