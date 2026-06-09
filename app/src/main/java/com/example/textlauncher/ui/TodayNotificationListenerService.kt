package com.example.textlauncher.ui

import android.service.notification.NotificationListenerService
import com.example.textlauncher.data.TodayNotificationCenter

class TodayNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        publishActiveNotifications()
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        publishActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        publishActiveNotifications()
    }

    override fun onListenerDisconnected() {
        TodayNotificationCenter.clear()
    }

    private fun publishActiveNotifications() {
        runCatching {
            TodayNotificationCenter.update(applicationContext, activeNotifications ?: emptyArray())
        }.onFailure {
            TodayNotificationCenter.clear()
        }
    }
}
