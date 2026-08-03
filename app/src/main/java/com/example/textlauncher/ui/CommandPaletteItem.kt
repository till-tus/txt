package com.example.textlauncher.ui

import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.QuickNote

internal sealed interface CommandPaletteItem {
    val stableKey: String
    val title: String
    val summary: String
    val searchText: String
    val category: Category

    data class App(
        val shortcut: AppShortcut,
        override val summary: String,
    ) : CommandPaletteItem {
        override val stableKey = "app:${shortcut.packageName}:${shortcut.activityName}"
        override val title = shortcut.label
        override val searchText = shortcut.label
        override val category = Category.App
    }

    data class Note(
        val note: QuickNote,
        override val summary: String,
    ) : CommandPaletteItem {
        override val stableKey = "note:${note.id}"
        override val title = note.text.lineSequence().firstOrNull().orEmpty().ifBlank { summary }
        override val searchText = note.text
        override val category = Category.Note
    }

    data class Event(
        val event: CalendarEvent,
        override val title: String,
        override val summary: String,
    ) : CommandPaletteItem {
        override val stableKey = "event:${event.id}:${event.startMillis}"
        override val searchText = "${event.title} ${event.calendarName}"
        override val category = Category.Event
    }

    data class Destination(
        val destination: PaletteDestination,
        override val title: String,
        override val summary: String,
        override val searchText: String = title,
        override val category: Category,
    ) : CommandPaletteItem {
        override val stableKey = "destination:${destination.name}"
    }

    enum class Category {
        App,
        Note,
        Event,
        Settings,
        Action,
        Page,
    }
}

internal enum class PaletteDestination {
    NotesPage,
    TodayPage,
    CalendarPage,
    Settings,
    AppearanceSettings,
    NotesSettings,
    CalendarSettings,
    GesturesSettings,
    ScreenTimeSettings,
    AddNote,
    ScreenTime,
}
