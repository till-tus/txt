package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.DeviceCalendar
import com.example.textlauncher.domain.ScreenTimeOverview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherDataSource(
    private val installedAppsRepository: InstalledAppsRepository,
    private val calendarRepository: CalendarRepository,
    private val screenTimeRepository: ScreenTimeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun loadLaunchableApps(): List<AppShortcut> {
        return withContext(computationDispatcher) {
            installedAppsRepository.loadLaunchableApps()
        }
    }

    suspend fun loadCalendars(): List<DeviceCalendar> {
        return withContext(ioDispatcher) {
            calendarRepository.loadCalendars()
        }
    }

    suspend fun loadUpcomingEvents(calendarIds: Set<Long>): List<CalendarEvent> {
        return withContext(ioDispatcher) {
            calendarRepository.loadUpcomingEvents(calendarIds)
        }
    }

    suspend fun loadScreenTimeOverview(excludedPackageNames: Set<String>): ScreenTimeOverview {
        return withContext(ioDispatcher) {
            screenTimeRepository.loadOverview(excludedPackageNames)
        }
    }

    suspend fun loadTodayUsageMillis(
        packageName: String,
        excludedPackageNames: Set<String>,
    ): Long {
        return withContext(ioDispatcher) {
            screenTimeRepository.loadTodayUsageMillis(packageName, excludedPackageNames)
        }
    }
}
