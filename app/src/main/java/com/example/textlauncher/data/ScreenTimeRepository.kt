package com.example.textlauncher.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        val packageUsage = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end,
        )
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }

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
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

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
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end,
        )
            .orEmpty()
            .sumOf { it.totalTimeInForeground }
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
    }
}
