package com.example.textlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageArrangementTest {
    @Test
    fun moveToOccupiedPosition_swapsPages() {
        val updated = PageArrangement.Default.move(LauncherPage.Today, PagePosition.Right)

        assertEquals(PagePosition.Right, updated.todayPosition)
        assertEquals(PagePosition.Down, updated.notesPosition)
        assertEquals(PagePosition.Left, updated.calendarPosition)
        assertTrue(updated.isValid())
    }

    @Test
    fun allowedPositions_excludeTheAppListDirection() {
        assertEquals(
            listOf(PagePosition.Left, PagePosition.Right, PagePosition.Down),
            PageArrangement.AllowedPositions,
        )
    }

    @Test
    fun duplicateStoredPositions_fallBackToDefault() {
        val updated = PageArrangement.validatedOrDefault(
            notesPosition = PagePosition.Left,
            todayPosition = PagePosition.Left,
            calendarPosition = PagePosition.Down,
        )

        assertEquals(PageArrangement.Default, updated)
    }

    @Test
    fun incompleteStoredPositions_fallBackToDefault() {
        val updated = PageArrangement.validatedOrDefault(
            notesPosition = null,
            todayPosition = PagePosition.Down,
            calendarPosition = PagePosition.Left,
        )

        assertEquals(PageArrangement.Default, updated)
    }
}
