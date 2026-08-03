package com.example.textlauncher.ui

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.QuickNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandPaletteSearchTest {
    @Test
    fun search_fuzzilyRanksAcrossResultTypes() {
        val results = CommandPaletteSearch.search(items, "calendr")

        assertEquals("Calendar", results.first().title)
        assertTrue(results.any { it is CommandPaletteItem.Event })
    }

    @Test
    fun search_supportsTypePrefixes() {
        val results = CommandPaletteSearch.search(items, "note: project")

        assertEquals(1, results.size)
        assertTrue(results.single() is CommandPaletteItem.Note)
    }

    @Test
    fun search_settingsPrefixOnlyReturnsSettingsDestinations() {
        val results = CommandPaletteSearch.search(items, "settings: calendar")

        assertEquals(listOf(CommandPaletteItem.Category.Settings), results.map { it.category }.distinct())
    }

    @Test
    fun search_prefersPinnedNoteWhenMatchesAreEqual() {
        val notes = listOf(
            note(1, "Project plan", isPinned = false),
            note(2, "Project plan", isPinned = true),
        )

        val results = CommandPaletteSearch.search(notes, "project")

        assertEquals(2L, (results.first() as CommandPaletteItem.Note).note.id)
    }

    @Test
    fun voiceNote_isUnavailableWhenNotesPageIsDisabled() {
        val voiceNote = QuickNote(id = 3, text = "", audioFileName = "voice-note.m4a")

        assertEquals(false, voiceNote.isAvailableInCommandPalette(notesPageEnabled = false))
        assertEquals(true, voiceNote.isAvailableInCommandPalette(notesPageEnabled = true))
    }

    @Test
    fun textNote_remainsAvailableWhenNotesPageIsDisabled() {
        val textNote = QuickNote(id = 4, text = "Project plan")

        assertEquals(true, textNote.isAvailableInCommandPalette(notesPageEnabled = false))
    }

    private companion object {
        val items = listOf(
            CommandPaletteItem.App(
                AppShortcut("Calculator", "calculator", "Calculator.Activity"),
                "App",
            ),
            CommandPaletteItem.App(
                AppShortcut("Calendar", "calendar", "Calendar.Activity"),
                "App",
            ),
            note(1, "Project calendar"),
            CommandPaletteItem.Event(
                CalendarEvent(2, "Calendar review", "Work", 1_000, 2_000, false),
                "Calendar review",
                "Tomorrow · Work",
            ),
            CommandPaletteItem.Destination(
                PaletteDestination.CalendarSettings,
                "Calendar & Today",
                "Settings",
                category = CommandPaletteItem.Category.Settings,
            ),
        )

        fun note(id: Long, text: String, isPinned: Boolean = false): CommandPaletteItem.Note {
            return CommandPaletteItem.Note(QuickNote(id, text, isPinned), if (isPinned) "Pinned note" else "Note")
        }
    }
}
