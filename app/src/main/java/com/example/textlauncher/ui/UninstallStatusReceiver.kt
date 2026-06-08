package com.example.textlauncher.ui

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.textlauncher.R

class UninstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = intent.getPendingUserActionIntent()
                if (confirmationIntent != null) {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmationIntent)
                } else {
                    launchSystemUninstallerFallback(context, intent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> launchSystemUninstallerFallback(context, intent)
        }
    }

    private fun Intent.getPendingUserActionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_INTENT)
        }
    }

    private fun launchSystemUninstallerFallback(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (packageName == null) {
            Toast.makeText(context, R.string.quick_access_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val packageUri = Uri.fromParts("package", packageName, null)
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, packageUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.quick_access_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_UNINSTALL_STATUS = "com.example.textlauncher.action.UNINSTALL_STATUS"
        const val EXTRA_PACKAGE_NAME = "com.example.textlauncher.extra.PACKAGE_NAME"
    }
}
