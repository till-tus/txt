package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.R
import com.example.textlauncher.databinding.ItemScreenTimeAppBinding
import com.example.textlauncher.domain.ScreenTimeAppUsage

class ScreenTimeAdapter : ListAdapter<ScreenTimeAppUsage, ScreenTimeAdapter.ScreenTimeViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenTimeViewHolder {
        return ScreenTimeViewHolder(
            ItemScreenTimeAppBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: ScreenTimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScreenTimeViewHolder(
        private val binding: ItemScreenTimeAppBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appUsage: ScreenTimeAppUsage) {
            binding.screenTimeAppName.text = appUsage.label
            binding.screenTimeUsage.text = formatUsageTime(appUsage.usageMillis)
        }

        private fun formatUsageTime(usageMillis: Long): String {
            val totalMinutes = (usageMillis / MILLIS_PER_MINUTE).coerceAtLeast(1)
            val hours = totalMinutes / MINUTES_PER_HOUR
            val minutes = totalMinutes % MINUTES_PER_HOUR
            return if (hours > 0) {
                binding.root.context.getString(R.string.duration_hours_minutes, hours, minutes)
            } else {
                binding.root.context.getString(R.string.duration_minutes, minutes)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScreenTimeAppUsage>() {
        override fun areItemsTheSame(oldItem: ScreenTimeAppUsage, newItem: ScreenTimeAppUsage): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: ScreenTimeAppUsage, newItem: ScreenTimeAppUsage): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_HOUR = 60L
    }
}
