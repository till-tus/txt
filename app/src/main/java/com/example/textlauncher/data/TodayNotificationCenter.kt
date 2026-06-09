package com.example.textlauncher.data

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import com.example.textlauncher.domain.TodayNotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TodayNotificationCenter {
    private val _notifications = MutableStateFlow<List<TodayNotificationItem>>(emptyList())
    val notifications: StateFlow<List<TodayNotificationItem>> = _notifications.asStateFlow()

    fun update(context: Context, activeNotifications: Array<StatusBarNotification>) {
        val packageManager = context.packageManager
        _notifications.value = activeNotifications
            .asSequence()
            .filterNot { it.packageName == context.packageName }
            .filterNot { it.isGroupSummary() }
            .mapNotNull { notification ->
                notification.toTodayNotificationItem(packageManager)
            }
            .sortedByDescending { it.postTimeMillis }
            .distinctBy { notification ->
                listOf(
                    notification.packageName,
                    notification.title,
                    notification.text,
                )
            }
            .toList()
    }

    fun clear() {
        _notifications.value = emptyList()
    }

    private fun StatusBarNotification.toTodayNotificationItem(
        packageManager: PackageManager,
    ): TodayNotificationItem? {
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()
            .trim()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()
            .trim()
        val appLabel = loadAppLabel(packageManager, packageName)
        if (title.isBlank() && text.isBlank() && appLabel.isBlank()) return null
        return TodayNotificationItem(
            packageName = packageName,
            appLabel = appLabel.ifBlank { packageName },
            title = title.ifBlank { appLabel.ifBlank { packageName } },
            text = text,
            postTimeMillis = postTime,
        )
    }

    private fun StatusBarNotification.isGroupSummary(): Boolean {
        return notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
    }

    private fun loadAppLabel(packageManager: PackageManager, packageName: String): String {
        return runCatching {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            applicationInfo.loadLabel(packageManager).toString()
        }.getOrElse { packageName }
    }
}
