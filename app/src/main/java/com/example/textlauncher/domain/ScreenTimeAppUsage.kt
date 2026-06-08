package com.example.textlauncher.domain

data class ScreenTimeAppUsage(
    val label: String,
    val packageName: String,
    val usageMillis: Long,
)
