package com.example.textlauncher.domain

data class TodayWidget(
    val id: String,
    val type: TodayWidgetType,
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
)

enum class TodayWidgetType {
    NextEvent,
}
