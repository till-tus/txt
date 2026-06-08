package com.example.textlauncher.domain

data class CalendarEvent(
    val id: Long,
    val title: String,
    val calendarName: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
)
