package com.example.textlauncher.ui

import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.QuickNote

data class HomeUiState(
    val shortcuts: List<AppShortcut> = emptyList(),
    val notes: List<QuickNote> = emptyList(),
    val showDate: Boolean = true,
    val clockDisplayMode: ClockDisplayMode = ClockDisplayMode.Analog,
    val showQuickAccess: Boolean = false,
    val maxShortcuts: Int = 5,
    val showScreenTimePage: Boolean = true,
    val showNotesPage: Boolean = true,
    val showCalendarPage: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val blockedAppPackageNames: Set<String> = emptySet(),
    val appBudgetMinutesByPackage: Map<String, Int> = emptyMap(),
    val hasRequestedCalendarPermission: Boolean = false,
)
