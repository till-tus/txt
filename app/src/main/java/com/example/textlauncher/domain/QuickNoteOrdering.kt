package com.example.textlauncher.domain

fun List<QuickNote>.sortedForNotesPage(): List<QuickNote> {
    return sortedWith(compareByDescending<QuickNote> { it.isPinned }.thenBy { it.id })
}
