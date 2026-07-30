package com.example.textlauncher.domain

enum class QuickAccessIcon {
    Camera,
    Notes,
    Calendar,
    Phone,
    Messages,
    Todos,
}

enum class QuickAccessPosition {
    BothRight,
    BothLeft,
    BothCenter,
    SplitEdges,
}

data class QuickAccessTarget(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: QuickAccessIcon,
)
