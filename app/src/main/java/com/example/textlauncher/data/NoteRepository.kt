package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import com.example.textlauncher.domain.QuickNote
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadNotes(): List<QuickNote> {
        val rawNotes = preferences.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val notes = JSONArray(rawNotes)
            List(notes.length()) { index ->
                val note = notes.getJSONObject(index)
                QuickNote(
                    id = note.getLong(FIELD_ID),
                    text = note.optString(FIELD_TEXT),
                    isPinned = note.optBoolean(FIELD_IS_PINNED, false),
                    audioFileName = note.optString(FIELD_AUDIO_FILE_NAME)
                        .takeIf { it.isNotBlank() },
                    audioDurationMillis = note.optLong(FIELD_AUDIO_DURATION_MILLIS, 0L),
                    audioWaveform = note.optJSONArray(FIELD_AUDIO_WAVEFORM)
                        ?.toIntList()
                        .orEmpty(),
                )
            }.withSinglePinnedNote()
        }.getOrElse { emptyList() }
    }

    fun saveNotes(notes: List<QuickNote>) {
        val normalizedNotes = notes.withSinglePinnedNote()
        val serialized = JSONArray().apply {
            normalizedNotes.forEach { note ->
                put(
                    JSONObject()
                        .put(FIELD_ID, note.id)
                        .put(FIELD_TEXT, note.text)
                        .put(FIELD_IS_PINNED, note.isPinned)
                        .put(FIELD_AUDIO_FILE_NAME, note.audioFileName.orEmpty())
                        .put(FIELD_AUDIO_DURATION_MILLIS, note.audioDurationMillis)
                        .put(FIELD_AUDIO_WAVEFORM, JSONArray(note.audioWaveform)),
                )
            }
        }
        preferences.edit {
            putString(KEY_NOTES, serialized.toString())
        }
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
        const val FIELD_ID = "id"
        const val FIELD_TEXT = "text"
        const val FIELD_IS_PINNED = "isPinned"
        const val FIELD_AUDIO_FILE_NAME = "audioFileName"
        const val FIELD_AUDIO_DURATION_MILLIS = "audioDurationMillis"
        const val FIELD_AUDIO_WAVEFORM = "audioWaveform"
    }
}
