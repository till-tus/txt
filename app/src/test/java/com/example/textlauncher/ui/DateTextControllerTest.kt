package com.example.textlauncher.ui

import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTextControllerTest {
    @Test
    fun formatDate_usesProvidedTimeZoneAndLocale() {
        val timestamp = Instant.parse("2026-07-30T23:30:00Z").toEpochMilli()

        assertEquals(
            "Thursday, July 30, 2026",
            DateTextController.formatDate(timestamp, Locale.US, TimeZone.getTimeZone("UTC")),
        )
        assertEquals(
            "Friday, July 31, 2026",
            DateTextController.formatDate(timestamp, Locale.US, TimeZone.getTimeZone("Europe/Berlin")),
        )
    }
}
