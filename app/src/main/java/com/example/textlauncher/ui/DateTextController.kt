package com.example.textlauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class DateTextController(
    private val context: Context,
    private val textView: TextView,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : DefaultLifecycleObserver {
    private var isRegistered = false
    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            render()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        render()
        if (isRegistered) return
        ContextCompat.registerReceiver(
            context,
            timeChangeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        isRegistered = true
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!isRegistered) return
        context.unregisterReceiver(timeChangeReceiver)
        isRegistered = false
    }

    private fun render() {
        val locale = context.resources.configuration.locales[0]
        textView.text = formatDate(
            timeMillis = nowMillis(),
            locale = locale,
            timeZone = TimeZone.getDefault(),
        )
    }

    internal companion object {
        fun formatDate(timeMillis: Long, locale: Locale, timeZone: TimeZone): String {
            return DateFormat.getDateInstance(DateFormat.FULL, locale).apply {
                this.timeZone = timeZone
            }.format(Date(timeMillis))
        }
    }
}
