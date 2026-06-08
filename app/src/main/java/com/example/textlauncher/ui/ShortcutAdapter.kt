package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemShortcutBinding
import com.example.textlauncher.domain.AppShortcut

class ShortcutAdapter(
    private val onShortcutClick: (View, AppShortcut) -> Unit,
    private val onShortcutLongClick: (View, AppShortcut) -> Unit,
) : ListAdapter<AppShortcut, ShortcutAdapter.ShortcutViewHolder>(DiffCallback) {
    var isEditMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShortcutViewHolder(binding, onShortcutClick, onShortcutLongClick) { isEditMode }
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShortcutViewHolder(
        private val binding: ItemShortcutBinding,
        private val onShortcutClick: (View, AppShortcut) -> Unit,
        private val onShortcutLongClick: (View, AppShortcut) -> Unit,
        private val isEditMode: () -> Boolean,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shortcut: AppShortcut) {
            binding.shortcutName.text = shortcut.label
            binding.root.setOnClickListener { onShortcutClick(it, shortcut) }
            if (isEditMode()) {
                binding.root.setOnLongClickListener(null)
            } else {
                binding.root.setOnLongClickListener {
                    onShortcutLongClick(it, shortcut)
                    true
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppShortcut>() {
        override fun areItemsTheSame(oldItem: AppShortcut, newItem: AppShortcut): Boolean {
            return oldItem.packageName == newItem.packageName &&
                oldItem.activityName == newItem.activityName &&
                oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: AppShortcut, newItem: AppShortcut): Boolean {
            return oldItem == newItem
        }
    }
}
