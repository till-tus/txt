package com.example.textlauncher.data

import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.TrashedNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteStoreStateTest {
    @Test
    fun moveToTrashPreservesFullVoiceNoteAndOriginalPosition() {
        val first = QuickNote(id = 1L, text = "First")
        val voiceNote = QuickNote(
            id = 2L,
            text = "",
            isPinned = true,
            audioFileName = "voice-2.m4a",
            audioDurationMillis = 12_500L,
            audioWaveform = listOf(4, 32, 81),
        )
        val state = NoteStoreState(notes = listOf(first, voiceNote))

        val updated = state.moveToTrash(noteId = voiceNote.id, deletedAtMillis = 99L)

        assertEquals(listOf(first), updated.notes)
        assertEquals(
            TrashedNote(
                note = voiceNote,
                originalPosition = 1,
                deletedAtMillis = 99L,
            ),
            updated.trash.single(),
        )
    }

    @Test
    fun restoreReturnsNoteToItsOriginalPosition() {
        val first = QuickNote(id = 1L, text = "First")
        val restored = QuickNote(id = 2L, text = "Second")
        val third = QuickNote(id = 3L, text = "Third")
        val trashed = TrashedNote(restored, originalPosition = 1, deletedAtMillis = 99L)
        val state = NoteStoreState(
            notes = listOf(first, third),
            trash = listOf(trashed),
        )

        val updated = state.restoreFromTrash(restored.id)

        assertEquals(listOf(first, restored, third), updated.notes)
        assertTrue(updated.trash.isEmpty())
    }

    @Test
    fun restoringMultipleNotesReappliesCanonicalOrder() {
        val first = QuickNote(id = 1L, text = "First")
        val second = QuickNote(id = 2L, text = "Second")
        val third = QuickNote(id = 3L, text = "Third")
        val afterDeletes = NoteStoreState(notes = listOf(first, second, third))
            .moveToTrash(noteId = second.id, deletedAtMillis = 10L)
            .moveToTrash(noteId = third.id, deletedAtMillis = 20L)

        val afterRestores = afterDeletes
            .restoreFromTrash(second.id)
            .restoreFromTrash(third.id)

        assertEquals(listOf(first, second, third), afterRestores.notes)
        assertTrue(afterRestores.trash.isEmpty())
    }

    @Test
    fun restoringPinnedNoteReplacesCurrentPin() {
        val currentPin = QuickNote(id = 1L, text = "Current", isPinned = true)
        val restoredPin = QuickNote(id = 2L, text = "Restored", isPinned = true)
        val state = NoteStoreState(
            notes = listOf(currentPin),
            trash = listOf(
                TrashedNote(restoredPin, originalPosition = 0, deletedAtMillis = 99L),
            ),
        )

        val updated = state.restoreFromTrash(restoredPin.id)

        assertEquals(restoredPin, updated.notes[0])
        assertFalse(updated.notes[1].isPinned)
        assertEquals(1, updated.notes.count { it.isPinned })
    }

    @Test
    fun permanentDeleteOnlyRemovesSelectedTrashEntry() {
        val first = TrashedNote(QuickNote(1L, "First"), 0, 10L)
        val second = TrashedNote(QuickNote(2L, "Second"), 1, 20L)
        val state = NoteStoreState(trash = listOf(second, first))

        val updated = state.permanentlyDeleteFromTrash(second.note.id)

        assertEquals(listOf(first), updated.trash)
        assertTrue(updated.notes.isEmpty())
    }

    @Test
    fun unknownOperationsLeaveStateUnchanged() {
        val active = QuickNote(id = 1L, text = "Active")
        val trashed = TrashedNote(QuickNote(2L, "Trashed"), 1, 20L)
        val state = NoteStoreState(notes = listOf(active), trash = listOf(trashed))

        assertSame(state, state.moveToTrash(noteId = 99L, deletedAtMillis = 50L))
        assertSame(state, state.restoreFromTrash(noteId = 99L))
        assertSame(state, state.permanentlyDeleteFromTrash(noteId = 99L))
    }
}
