package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemShortcutBinding
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.ShortcutTextAlignment

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
    var shortcutTextAlignment: ShortcutTextAlignment = ShortcutTextAlignment.Left
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.shortcutName.minWidth =
            parent.resources.displayMetrics.widthPixels / MINIMUM_TAP_TARGET_SCREEN_FRACTION
        binding.shortcutName.maxWidth = parent.resources.displayMetrics.widthPixels -
            parent.paddingStart -
            parent.paddingEnd
        return ShortcutViewHolder(
            binding,
            onShortcutClick,
            onShortcutLongClick,
            { isEditMode },
            { shortcutTextAlignment },
        )
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShortcutViewHolder(
        private val binding: ItemShortcutBinding,
        private val onShortcutClick: (View, AppShortcut) -> Unit,
        private val onShortcutLongClick: (View, AppShortcut) -> Unit,
        private val isEditMode: () -> Boolean,
        private val shortcutTextAlignment: () -> ShortcutTextAlignment,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shortcut: AppShortcut) {
            applyShortcutTextAlignment()
            binding.shortcutName.text = shortcut.label
            binding.shortcutName.setOnClickListener { onShortcutClick(it, shortcut) }
            if (isEditMode()) {
                binding.shortcutName.setOnLongClickListener(null)
            } else {
                binding.shortcutName.setOnLongClickListener {
                    onShortcutLongClick(it, shortcut)
                    true
                }
            }
        }

        private fun applyShortcutTextAlignment() {
            val alignment = shortcutTextAlignment()
            binding.shortcutName.gravity = Gravity.CENTER_VERTICAL or when (alignment) {
                ShortcutTextAlignment.Left -> Gravity.START
                ShortcutTextAlignment.Center -> Gravity.CENTER_HORIZONTAL
                ShortcutTextAlignment.Right -> Gravity.END
            }
            val params = binding.shortcutName.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER_VERTICAL or when (alignment) {
                ShortcutTextAlignment.Left -> Gravity.START
                ShortcutTextAlignment.Center -> Gravity.CENTER_HORIZONTAL
                ShortcutTextAlignment.Right -> Gravity.END
            }
            binding.shortcutName.layoutParams = params
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

    private companion object {
        const val MINIMUM_TAP_TARGET_SCREEN_FRACTION = 8
    }
}
