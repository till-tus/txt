package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemNoteBinding
import com.example.textlauncher.domain.QuickNote

class NoteAdapter(
    private val onNoteClick: (QuickNote) -> Unit,
    private val onCopyClick: (QuickNote) -> Unit,
    private val onNoteLongClick: (View, QuickNote) -> Unit,
) : ListAdapter<QuickNote, NoteAdapter.NoteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: QuickNote) {
            binding.noteText.text = note.text
            binding.pinnedNoteDot.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onNoteClick(note) }
            binding.root.setOnLongClickListener {
                onNoteLongClick(binding.root, note)
                true
            }
            binding.copyNoteButton.setOnClickListener { onCopyClick(note) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuickNote>() {
        override fun areItemsTheSame(oldItem: QuickNote, newItem: QuickNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuickNote, newItem: QuickNote): Boolean {
            return oldItem == newItem
        }
    }
}
