package com.example.textlauncher.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import com.example.textlauncher.domain.ScreenTimeOverview
import java.text.SimpleDateFormat
import java.util.Calendar

class ScreenTimeRepository(
    private val context: Context,
) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun loadOverview(excludedPackageNames: Set<String> = emptySet()): ScreenTimeOverview {
        val now = System.currentTimeMillis()
        val weekStart = startOfCurrentWeek()
        val todayStart = startOfToday()
        val events = loadEvents(weekStart - EVENT_LOOKBACK_MILLIS, now)
        val ignoredPackageNames = excludedPackageNames + context.packageName
        val todayUsage = calculateUsage(
            events = events,
            start = todayStart,
            end = now,
            ignoredPackageNames = ignoredPackageNames,
        ).toAppUsage()
        val dayFormat = SimpleDateFormat("EEE", context.resources.configuration.locales[0])
        val weekUsage = (0 until DAYS_PER_WEEK).map { offset ->
            val dayStartCalendar = Calendar.getInstance().apply {
                timeInMillis = weekStart
                add(Calendar.DAY_OF_WEEK, offset)
            }
            val dayStart = dayStartCalendar.timeInMillis
            val dayEnd = (dayStartCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_WEEK, 1)
            }.timeInMillis
            val isElapsed = dayStart <= now
            val usageMillis = if (isElapsed) {
                calculateUsage(
                    events = events,
                    start = dayStart,
                    end = minOf(dayEnd, now),
                    ignoredPackageNames = ignoredPackageNames,
                ).values.sum()
            } else {
                0L
            }
            ScreenTimeDayUsage(
                label = dayFormat.format(dayStartCalendar.time),
                usageMillis = usageMillis,
                isElapsed = isElapsed,
            )
        }
        return ScreenTimeOverview(today = todayUsage, week = weekUsage)
    }

    fun loadTodayUsage(excludedPackageNames: Set<String> = emptySet()): List<ScreenTimeAppUsage> {
        val start = startOfToday()
        val end = System.currentTimeMillis()
        val packageUsage = loadUsageByPackage(start, end, excludedPackageNames)

        return packageUsage.toAppUsage()
    }

    fun loadTodayUsageMillis(
        packageName: String,
        excludedPackageNames: Set<String> = emptySet(),
    ): Long {
        return loadUsageByPackage(
            start = startOfToday(),
            end = System.currentTimeMillis(),
            excludedPackageNames = excludedPackageNames,
        )[packageName] ?: 0L
    }

    fun loadCurrentWeekUsage(excludedPackageNames: Set<String> = emptySet()): List<ScreenTimeDayUsage> {
        return loadOverview(excludedPackageNames).week
    }

    private fun loadUsageByPackage(
        start: Long,
        end: Long,
        excludedPackageNames: Set<String>,
    ): Map<String, Long> {
        if (end <= start) return emptyMap()
        return calculateUsage(
            events = loadEvents(start - EVENT_LOOKBACK_MILLIS, end),
            start = start,
            end = end,
            ignoredPackageNames = excludedPackageNames + context.packageName,
        )
    }

    private fun loadEvents(start: Long, end: Long): List<ScreenTimeUsageEvent> {
        val events = mutableListOf<ScreenTimeUsageEvent>()
        val usageEvents = usageStatsManager.queryEvents(start, end)
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
        return events
    }

    private fun calculateUsage(
        events: List<ScreenTimeUsageEvent>,
        start: Long,
        end: Long,
        ignoredPackageNames: Set<String>,
    ): Map<String, Long> {
        if (end <= start) return emptyMap()
        return ScreenTimeUsageCalculator.calculatePackageUsage(
            events = events,
            startMillis = start,
            endMillis = end,
            ignoredPackageNames = ignoredPackageNames,
        )
    }

    private fun Map<String, Long>.toAppUsage(): List<ScreenTimeAppUsage> {
        return asSequence()
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

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfCurrentWeek(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                add(Calendar.DAY_OF_WEEK, -1)
            }
        }.timeInMillis
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
