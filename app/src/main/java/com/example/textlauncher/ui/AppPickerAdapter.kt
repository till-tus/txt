package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemPickerAppBinding
import com.example.textlauncher.domain.AppShortcut

class AppPickerAdapter(
    private val onAppClick: (AppShortcut) -> Unit,
    private val onAppLongClick: (View, AppShortcut) -> Unit,
) : ListAdapter<AppShortcut, AppPickerAdapter.AppViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemPickerAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding, onAppClick, onAppLongClick)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppViewHolder(
        private val binding: ItemPickerAppBinding,
        private val onAppClick: (AppShortcut) -> Unit,
        private val onAppLongClick: (View, AppShortcut) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shortcut: AppShortcut) {
            binding.appName.text = shortcut.label
            binding.root.setOnClickListener { onAppClick(shortcut) }
            binding.root.setOnLongClickListener {
                onAppLongClick(it, shortcut)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppShortcut>() {
        override fun areItemsTheSame(oldItem: AppShortcut, newItem: AppShortcut): Boolean {
            return oldItem.packageName == newItem.packageName &&
                oldItem.activityName == newItem.activityName
        }

        override fun areContentsTheSame(oldItem: AppShortcut, newItem: AppShortcut): Boolean {
            return oldItem == newItem
        }
    }
}
