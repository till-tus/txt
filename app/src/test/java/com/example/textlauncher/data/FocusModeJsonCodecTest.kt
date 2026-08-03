package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.FocusMode
import com.example.textlauncher.domain.FocusSchedule
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusModeJsonCodecTest {
    @Test
    fun roundTrip_preservesAllConfiguredValues() {
        val modes = listOf(
            FocusMode(
                id = "deep-work",
                name = "Deep work",
                blockedAppPackageNames = setOf("social.app"),
                appBudgetMinutesByPackage = mapOf("mail.app" to 15),
                shortcuts = listOf(AppShortcut("Docs", "docs.app", "DocsActivity")),
                schedule = FocusSchedule(
                    enabled = true,
                    daysOfWeek = setOf(1, 2, 3, 4, 5),
                    startMinuteOfDay = 8 * 60 + 30,
                    endMinuteOfDay = 12 * 60,
                ),
            ),
        )

        assertEquals(modes, FocusModeJsonCodec.decodeOrEmpty(FocusModeJsonCodec.encode(modes)))
    }

    @Test
    fun malformedJson_returnsEmptyList() {
        assertEquals(emptyList<FocusMode>(), FocusModeJsonCodec.decodeOrEmpty("not-json"))
    }
}
