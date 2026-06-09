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
                    text = note.getString(FIELD_TEXT),
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveNotes(notes: List<QuickNote>) {
        val serialized = JSONArray().apply {
            notes.forEach { note ->
                put(
                    JSONObject()
                        .put(FIELD_ID, note.id)
                        .put(FIELD_TEXT, note.text),
                )
            }
        }
        preferences.edit {
            putString(KEY_NOTES, serialized.toString())
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "quick_notes"
        const val KEY_NOTES = "notes"
        const val FIELD_ID = "id"
        const val FIELD_TEXT = "text"
    }
}
