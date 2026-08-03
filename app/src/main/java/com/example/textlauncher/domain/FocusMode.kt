package com.example.textlauncher.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class FocusSchedule(
    val enabled: Boolean = false,
    val daysOfWeek: Set<Int> = emptySet(),
    val startMinuteOfDay: Int = 9 * 60,
    val endMinuteOfDay: Int = 17 * 60,
) {
    fun isActiveAt(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!enabled || daysOfWeek.isEmpty()) return false

        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val minuteOfDay = dateTime.hour * 60 + dateTime.minute
        val today = dateTime.dayOfWeek.value
        val start = startMinuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        val end = endMinuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        return when {
            start == end -> today in daysOfWeek
            start < end -> today in daysOfWeek && minuteOfDay in start until end
            else -> {
                val yesterday = dateTime.dayOfWeek.minusOne().value
                (today in daysOfWeek && minuteOfDay >= start) ||
                    (yesterday in daysOfWeek && minuteOfDay < end)
            }
        }
    }

    private fun DayOfWeek.minusOne(): DayOfWeek {
        return if (this == DayOfWeek.MONDAY) DayOfWeek.SUNDAY else DayOfWeek.of(value - 1)
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

data class FocusMode(
    val id: String,
    val name: String,
    val blockedAppPackageNames: Set<String> = emptySet(),
    val appBudgetMinutesByPackage: Map<String, Int> = emptyMap(),
    val shortcuts: List<AppShortcut> = emptyList(),
    val schedule: FocusSchedule = FocusSchedule(),
)

enum class FocusActivationSource {
    Manual,
    Schedule,
}

data class ActiveFocusMode(
    val mode: FocusMode,
    val source: FocusActivationSource,
)

object FocusModeResolver {
    fun resolve(
        focusModesEnabled: Boolean,
        focusModes: List<FocusMode>,
        manuallyActiveFocusModeId: String?,
        schedulesPausedUntilEpochMillis: Long,
        nowEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ActiveFocusMode? {
        if (!focusModesEnabled) return null

        focusModes.firstOrNull { it.id == manuallyActiveFocusModeId }?.let { mode ->
            return ActiveFocusMode(mode, FocusActivationSource.Manual)
        }
        if (nowEpochMillis < schedulesPausedUntilEpochMillis) return null

        return focusModes.firstOrNull { mode -> mode.schedule.isActiveAt(nowEpochMillis, zoneId) }
            ?.let { mode -> ActiveFocusMode(mode, FocusActivationSource.Schedule) }
    }
}
