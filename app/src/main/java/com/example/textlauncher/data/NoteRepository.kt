package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.TrashedNote
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadState(): NoteStoreState {
        return NoteStoreState(
            notes = loadNotes(),
            trash = loadTrash(),
        )
    }

    fun loadNotes(): List<QuickNote> {
        val rawNotes = preferences.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val notes = JSONArray(rawNotes)
            List(notes.length()) { index ->
                notes.getJSONObject(index).toQuickNote()
            }.withSinglePinnedNote()
        }.getOrElse { emptyList() }
    }

    fun loadTrash(): List<TrashedNote> {
        val rawTrash = preferences.getString(KEY_TRASH_V1, null) ?: return emptyList()
        return runCatching {
            val trash = JSONArray(rawTrash)
            List(trash.length()) { index ->
                val trashedNote = trash.getJSONObject(index)
                TrashedNote(
                    note = trashedNote.getJSONObject(FIELD_NOTE).toQuickNote(),
                    originalPosition = trashedNote.optInt(FIELD_ORIGINAL_POSITION, 0)
                        .coerceAtLeast(0),
                    deletedAtMillis = trashedNote.optLong(FIELD_DELETED_AT_MILLIS, 0L)
                        .coerceAtLeast(0L),
                )
            }.distinctBy { it.note.id }
        }.getOrElse { emptyList() }
    }

    fun saveNotes(notes: List<QuickNote>) {
        saveState(NoteStoreState(notes = notes, trash = loadTrash()))
    }

    fun saveState(state: NoteStoreState) {
        val serializedNotes = JSONArray().apply {
            state.notes.withSinglePinnedNote().forEach { note ->
                put(note.toJson())
            }
        }
        val serializedTrash = JSONArray().apply {
            state.trash.distinctBy { it.note.id }.forEach { trashedNote ->
                put(
                    JSONObject()
                        .put(FIELD_NOTE, trashedNote.note.toJson())
                        .put(FIELD_ORIGINAL_POSITION, trashedNote.originalPosition.coerceAtLeast(0))
                        .put(FIELD_DELETED_AT_MILLIS, trashedNote.deletedAtMillis.coerceAtLeast(0L)),
                )
            }
        }
        preferences.edit {
            putString(KEY_NOTES, serializedNotes.toString())
            putString(KEY_TRASH_V1, serializedTrash.toString())
        }
    }

    private fun QuickNote.toJson(): JSONObject {
        return JSONObject()
            .put(FIELD_ID, id)
            .put(FIELD_TEXT, text)
            .put(FIELD_IS_PINNED, isPinned)
            .put(FIELD_AUDIO_FILE_NAME, audioFileName.orEmpty())
            .put(FIELD_AUDIO_DURATION_MILLIS, audioDurationMillis)
            .put(FIELD_AUDIO_WAVEFORM, JSONArray(audioWaveform))
    }

    private fun JSONObject.toQuickNote(): QuickNote {
        return QuickNote(
            id = getLong(FIELD_ID),
            text = optString(FIELD_TEXT),
            isPinned = optBoolean(FIELD_IS_PINNED, false),
            audioFileName = optString(FIELD_AUDIO_FILE_NAME)
                .takeIf { it.isNotBlank() },
            audioDurationMillis = optLong(FIELD_AUDIO_DURATION_MILLIS, 0L),
            audioWaveform = optJSONArray(FIELD_AUDIO_WAVEFORM)
                ?.toIntList()
                .orEmpty(),
        )
    }

    private fun List<QuickNote>.withSinglePinnedNote(): List<QuickNote> {
        var hasPinnedNote = false
        return map { note ->
            if (!note.isPinned) {
                note
            } else if (!hasPinnedNote) {
                hasPinnedNote = true
                note
            } else {
                note.copy(isPinned = false)
            }
        }.sortedWith(compareByDescending<QuickNote> { it.isPinned }.thenBy { it.id })
    }

    private fun JSONArray.toIntList(): List<Int> {
        return List(length()) { index -> optInt(index) }
            .map { it.coerceIn(0, 100) }
    }

    private companion object {
        const val PREFERENCES_NAME = "quick_notes"
        const val KEY_NOTES = "notes"
        const val KEY_TRASH_V1 = "trash_v1"
        const val FIELD_ID = "id"
        const val FIELD_TEXT = "text"
        const val FIELD_IS_PINNED = "isPinned"
        const val FIELD_AUDIO_FILE_NAME = "audioFileName"
        const val FIELD_AUDIO_DURATION_MILLIS = "audioDurationMillis"
        const val FIELD_AUDIO_WAVEFORM = "audioWaveform"
        const val FIELD_NOTE = "note"
        const val FIELD_ORIGINAL_POSITION = "originalPosition"
        const val FIELD_DELETED_AT_MILLIS = "deletedAtMillis"
    }
}
