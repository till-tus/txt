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
    fun moveToEmptyPosition_leavesPreviousPositionEmpty() {
        val updated = PageArrangement.Default.move(LauncherPage.Calendar, PagePosition.Up)

        assertEquals(PagePosition.Up, updated.calendarPosition)
        assertEquals(null, updated.pageAt(PagePosition.Left))
        assertTrue(updated.isValid())
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
}
