package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.R
import com.example.textlauncher.databinding.ItemNoteBinding
import com.example.textlauncher.domain.QuickNote
import java.util.Locale

class NoteAdapter(
    private val onNoteClick: (QuickNote) -> Unit,
    private val onCopyClick: (QuickNote) -> Unit,
    private val onNoteLongClick: (View, QuickNote) -> Unit,
    private val onVoiceNotePlayClick: (QuickNote) -> Unit,
    private val onVoiceNotePlayLongClick: (QuickNote) -> Unit,
) : ListAdapter<QuickNote, NoteAdapter.NoteViewHolder>(DiffCallback) {
    var playingVoiceNoteId: Long? = null
        set(value) {
            val previous = field
            field = value
            notifyVoiceNoteChanged(previous)
            notifyVoiceNoteChanged(value)
        }
    var isVoicePlaybackPlaying: Boolean = false
        set(value) {
            field = value
            notifyVoiceNoteChanged(playingVoiceNoteId)
        }
    var voicePlaybackProgressFraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            notifyVoiceNoteChanged(playingVoiceNoteId)
        }

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
            val isVoiceNote = note.audioFileName != null
            binding.noteText.visibility = if (isVoiceNote) View.GONE else View.VISIBLE
            binding.voiceNoteRow.visibility = if (isVoiceNote) View.VISIBLE else View.GONE
            binding.copyNoteButton.visibility = if (isVoiceNote) View.GONE else View.VISIBLE
            binding.noteText.text = note.text
            binding.pinnedNoteDot.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                if (isVoiceNote) {
                    onVoiceNotePlayClick(note)
                } else {
                    onNoteClick(note)
                }
            }
            binding.root.setOnLongClickListener {
                onNoteLongClick(binding.root, note)
                true
            }
            binding.copyNoteButton.setOnClickListener { onCopyClick(note) }
            if (isVoiceNote) {
                val isActive = note.id == playingVoiceNoteId
                val isPlaying = isActive && isVoicePlaybackPlaying
                binding.playVoiceNoteButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                binding.playVoiceNoteButton.contentDescription = binding.root.context.getString(
                    if (isPlaying) R.string.pause_voice_note else R.string.play_voice_note,
                )
                binding.playVoiceNoteButton.setOnClickListener { onVoiceNotePlayClick(note) }
                binding.playVoiceNoteButton.setOnLongClickListener {
                    onVoiceNotePlayLongClick(note)
                    true
                }
                binding.voiceWaveform.samples = note.audioWaveform
                binding.voiceWaveform.progressFraction = if (isActive) voicePlaybackProgressFraction else 0f
                binding.voiceNoteDuration.text = formatDuration(note.audioDurationMillis)
            } else {
                binding.playVoiceNoteButton.setOnClickListener(null)
                binding.playVoiceNoteButton.setOnLongClickListener(null)
                binding.voiceWaveform.samples = emptyList()
                binding.voiceWaveform.progressFraction = 0f
                binding.voiceNoteDuration.text = ""
            }
        }
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(1L)
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun notifyVoiceNoteChanged(noteId: Long?) {
        if (noteId == null) return
        val index = currentList.indexOfFirst { it.id == noteId }
        if (index != -1) {
            notifyItemChanged(index)
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
