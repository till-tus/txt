package com.example.textlauncher.domain

data class ScreenTimeOverview(
    val today: List<ScreenTimeAppUsage>,
    val week: List<ScreenTimeDayUsage>,
)
