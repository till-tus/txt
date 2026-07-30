package com.example.textlauncher.data

import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.TrashedNote

data class NoteStoreState(
    val notes: List<QuickNote> = emptyList(),
    val trash: List<TrashedNote> = emptyList(),
) {
    fun moveToTrash(noteId: Long, deletedAtMillis: Long): NoteStoreState {
        val originalPosition = notes.indexOfFirst { it.id == noteId }
        if (originalPosition == -1) return this

        val note = notes[originalPosition]
        return copy(
            notes = notes.filterNot { it.id == noteId },
            trash = listOf(
                TrashedNote(
                    note = note,
                    originalPosition = originalPosition,
                    deletedAtMillis = deletedAtMillis,
                ),
            ) + trash.filterNot { it.note.id == noteId },
        )
    }

    fun restoreFromTrash(noteId: Long): NoteStoreState {
        val trashedNote = trash.firstOrNull { it.note.id == noteId } ?: return this
        if (notes.any { it.id == noteId }) return this

        val restoredNotes = notes
            .map { note ->
                if (trashedNote.note.isPinned && note.isPinned) {
                    note.copy(isPinned = false)
                } else {
                    note
                }
            }
            .toMutableList()
            .apply {
                add(trashedNote.originalPosition.coerceIn(0, size), trashedNote.note)
            }
        return copy(
            notes = restoredNotes,
            trash = trash.filterNot { it.note.id == noteId },
        )
    }

    fun permanentlyDeleteFromTrash(noteId: Long): NoteStoreState {
        if (trash.none { it.note.id == noteId }) return this
        return copy(trash = trash.filterNot { it.note.id == noteId })
    }
}
