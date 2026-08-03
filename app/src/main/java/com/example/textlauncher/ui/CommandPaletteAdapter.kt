package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemCommandPaletteResultBinding

internal class CommandPaletteAdapter(
    private val onItemClick: (CommandPaletteItem) -> Unit,
    private val onItemLongClick: (View, CommandPaletteItem) -> Unit,
) : ListAdapter<CommandPaletteItem, CommandPaletteAdapter.ItemViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            binding = ItemCommandPaletteResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(
        private val binding: ItemCommandPaletteResultBinding,
        private val onItemClick: (CommandPaletteItem) -> Unit,
        private val onItemLongClick: (View, CommandPaletteItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommandPaletteItem) {
            binding.commandTitle.text = item.title
            binding.commandSummary.text = item.summary
            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener { view ->
                onItemLongClick(view, item)
                item is CommandPaletteItem.App
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CommandPaletteItem>() {
        override fun areItemsTheSame(oldItem: CommandPaletteItem, newItem: CommandPaletteItem): Boolean {
            return oldItem.stableKey == newItem.stableKey
        }

        override fun areContentsTheSame(oldItem: CommandPaletteItem, newItem: CommandPaletteItem): Boolean {
            return oldItem == newItem
        }
    }
}
