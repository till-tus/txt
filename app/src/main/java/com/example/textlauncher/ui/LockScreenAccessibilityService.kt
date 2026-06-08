package com.example.textlauncher.ui

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockScreenAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        private var instance: LockScreenAccessibilityService? = null

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
        }
    }
}
