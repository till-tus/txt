package com.example.textlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBulletFormatterTest {
    private val formatter = NoteBulletFormatter()

    @Test
    fun didInsertNewline_returnsTrueOnlyWhenNewlineWasInserted() {
        assertTrue(formatter.didInsertNewline("- task\n", start = 6, before = 0, count = 1))
        assertFalse(formatter.didInsertNewline("- task\n", start = 6, before = 1, count = 0))
        assertFalse(formatter.didInsertNewline("- task", start = 6, before = 0, count = 0))
        assertFalse(formatter.didInsertNewline("- task", start = 5, before = 0, count = 1))
    }

    @Test
    fun formatAfterNewline_insertsBulletAfterNonEmptyBulletLine() {
        val result = formatter.formatAfterNewline("- task\n", cursorPosition = 7)

        assertEquals(
            NoteBulletFormatter.FormatResult.InsertBullet(index = 7, selection = 9),
            result,
        )
    }

    @Test
    fun formatAfterNewline_deletesEmptyBulletWhenStartingAnotherNewLine() {
        val result = formatter.formatAfterNewline("- task\n- \n", cursorPosition = 10)

        assertEquals(
            NoteBulletFormatter.FormatResult.DeleteEmptyBullet(start = 7, end = 9, selection = 8),
            result,
        )
    }
}
