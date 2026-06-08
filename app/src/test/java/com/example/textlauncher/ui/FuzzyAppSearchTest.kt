package com.example.textlauncher.ui

import com.example.textlauncher.domain.AppShortcut
import org.junit.Assert.assertEquals
import org.junit.Test

class FuzzyAppSearchTest {
    @Test
    fun filter_returnsSubstringMatchesFirst() {
        val result = FuzzyAppSearch.filter(apps, "calc")

        assertEquals("Calculator", result.first().label)
    }

    @Test
    fun filter_allowsSmallTypos() {
        val result = FuzzyAppSearch.filter(apps, "whatsap")

        assertEquals("WhatsApp", result.first().label)
    }

    @Test
    fun filter_allowsSubsequenceQueries() {
        val result = FuzzyAppSearch.filter(apps, "gmps")

        assertEquals("Google Maps", result.first().label)
    }

    private companion object {
        val apps = listOf(
            app("Calculator"),
            app("Calendar"),
            app("Google Maps"),
            app("WhatsApp"),
        )

        fun app(label: String): AppShortcut {
            return AppShortcut(
                label = label,
                packageName = label.lowercase().replace(" ", "."),
                activityName = "$label.Activity",
            )
        }
    }
}
