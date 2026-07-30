package com.example.textlauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.textlauncher.data.LauncherSettingsRepository
import com.example.textlauncher.data.ShortcutRepository

class PackageRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        ShortcutRepository(context.applicationContext).deleteShortcutsForPackage(packageName)
        LauncherSettingsRepository(context.applicationContext).removePackageReferences(packageName)
    }
}
