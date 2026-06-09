package com.example.textlauncher.domain

data class TodayNotificationItem(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTimeMillis: Long,
)
