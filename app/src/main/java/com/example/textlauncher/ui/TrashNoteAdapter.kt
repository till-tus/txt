package com.example.textlauncher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.R
import com.example.textlauncher.databinding.ItemTrashedNoteBinding
import com.example.textlauncher.domain.TrashedNote
import java.text.DateFormat
import java.util.Date

class TrashNoteAdapter(
    private val onRestoreClick: (TrashedNote) -> Unit,
    private val onDeletePermanentlyClick: (TrashedNote) -> Unit,
) : ListAdapter<TrashedNote, TrashNoteAdapter.TrashedNoteViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashedNoteViewHolder {
        return TrashedNoteViewHolder(
            ItemTrashedNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: TrashedNoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrashedNoteViewHolder(
        private val binding: ItemTrashedNoteBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trashedNote: TrashedNote) {
            val context = binding.root.context
            binding.trashedNoteText.text = trashedNote.note.audioFileName?.let {
                context.getString(
                    R.string.trashed_voice_note_summary,
                    formatDuration(trashedNote.note.audioDurationMillis, context),
                )
            } ?: trashedNote.note.text
            val deletedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(trashedNote.deletedAtMillis))
            binding.trashedNoteDeletedAt.text = context.getString(R.string.note_deleted_at, deletedAt)
            binding.restoreTrashedNoteButton.setOnClickListener {
                onRestoreClick(trashedNote)
            }
            binding.deleteTrashedNoteButton.setOnClickListener {
                onDeletePermanentlyClick(trashedNote)
            }
        }
    }

    private fun formatDuration(durationMillis: Long, context: Context): String {
        val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(1L)
        return context.getString(
            R.string.voice_note_duration,
            totalSeconds / 60L,
            totalSeconds % 60L,
        )
    }

    private object DiffCallback : DiffUtil.ItemCallback<TrashedNote>() {
        override fun areItemsTheSame(oldItem: TrashedNote, newItem: TrashedNote): Boolean {
            return oldItem.note.id == newItem.note.id
        }

        override fun areContentsTheSame(oldItem: TrashedNote, newItem: TrashedNote): Boolean {
            return oldItem == newItem
        }
    }
}
