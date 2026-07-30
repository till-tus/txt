package com.example.textlauncher.ui

import androidx.annotation.StringRes
import com.example.textlauncher.R

internal enum class SettingsPage(@param:StringRes val titleRes: Int) {
    Index(R.string.launcher_settings),
    Appearance(R.string.settings_category_appearance),
    Notes(R.string.settings_category_notes),
    Calendar(R.string.settings_category_calendar),
    Gestures(R.string.settings_category_gestures),
    ScreenTime(R.string.settings_category_screen_time),
}
