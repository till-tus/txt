package com.example.textlauncher.domain

data class TodayWidget(
    val id: String,
    val type: TodayWidgetType,
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val notificationAppPackageNames: Set<String> = emptySet(),
)

enum class TodayWidgetType {
    NextEvent,
    Weather,
    NotificationFeed,
    PinnedNote,
}
