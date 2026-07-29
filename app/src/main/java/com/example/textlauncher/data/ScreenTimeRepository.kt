package com.example.textlauncher.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import java.text.SimpleDateFormat
import java.util.Calendar

class ScreenTimeRepository(
    private val context: Context,
) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun loadTodayUsage(): List<ScreenTimeAppUsage> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val packageUsage = loadUsageByPackage(start, end)

        return packageUsage
            .asSequence()
            .filter { (_, usageMillis) -> usageMillis > 0 }
            .map { (packageName, usageMillis) ->
                ScreenTimeAppUsage(
                    label = loadAppLabel(packageName),
                    packageName = packageName,
                    usageMillis = usageMillis,
                )
            }
            .sortedWith(
                compareByDescending<ScreenTimeAppUsage> { it.usageMillis }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
            .toList()
    }

    fun loadCurrentWeekUsage(): List<ScreenTimeDayUsage> {
        val now = System.currentTimeMillis()
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                add(Calendar.DAY_OF_WEEK, -1)
            }
        }
        val dayFormat = SimpleDateFormat("EEE", context.resources.configuration.locales[0])

        return (0 until DAYS_PER_WEEK).map { offset ->
            val dayStartCalendar = weekStart.clone() as Calendar
            dayStartCalendar.add(Calendar.DAY_OF_WEEK, offset)
            val dayStart = dayStartCalendar.timeInMillis
            val dayEnd = (dayStartCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_WEEK, 1)
            }.timeInMillis
            val isElapsed = dayStart <= now
            val usageMillis = if (isElapsed) {
                loadUsageTotal(dayStart, minOf(dayEnd, now))
            } else {
                0L
            }

            ScreenTimeDayUsage(
                label = dayFormat.format(dayStartCalendar.time),
                usageMillis = usageMillis,
                isElapsed = isElapsed,
            )
        }
    }

    private fun loadUsageTotal(start: Long, end: Long): Long {
        if (end <= start) return 0L
        return loadUsageByPackage(start, end).values.sum()
    }

    private fun loadUsageByPackage(start: Long, end: Long): Map<String, Long> {
        if (end <= start) return emptyMap()
        val events = mutableListOf<ScreenTimeUsageEvent>()
        val usageEvents = usageStatsManager.queryEvents(start - EVENT_LOOKBACK_MILLIS, end)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = event.toScreenTimeUsageEventType() ?: continue
            val packageName = event.packageName?.takeIf { it.isNotBlank() } ?: continue
            events += ScreenTimeUsageEvent(
                packageName = packageName,
                timestampMillis = event.timeStamp,
                type = type,
            )
        }
        return ScreenTimeUsageCalculator.calculatePackageUsage(
            events = events,
            startMillis = start,
            endMillis = end,
            ignoredPackageNames = setOf(context.packageName),
        )
    }

    private fun UsageEvents.Event.toScreenTimeUsageEventType(): ScreenTimeUsageEventType? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> ScreenTimeUsageEventType.Foreground
                UsageEvents.Event.ACTIVITY_PAUSED -> ScreenTimeUsageEventType.Background
                else -> null
            }
        } else {
            when (eventType) {
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_FOREGROUND -> ScreenTimeUsageEventType.Foreground
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_BACKGROUND -> ScreenTimeUsageEventType.Background
                else -> null
            }
        }
    }

    private fun loadAppLabel(packageName: String): String {
        val packageManager = context.packageManager
        return runCatching {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrElse { packageName }
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val EVENT_LOOKBACK_MILLIS = 7L * 24L * 60L * 60L * 1_000L
    }
}
