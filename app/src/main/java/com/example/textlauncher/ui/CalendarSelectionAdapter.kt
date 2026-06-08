package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemCalendarSelectionBinding
import com.example.textlauncher.domain.DeviceCalendar

class CalendarSelectionAdapter(
    private val onCalendarToggled: (DeviceCalendar, Boolean) -> Unit,
) : ListAdapter<CalendarSelectionItem, CalendarSelectionAdapter.CalendarViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        return CalendarViewHolder(
            ItemCalendarSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CalendarViewHolder(
        private val binding: ItemCalendarSelectionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CalendarSelectionItem) {
            binding.calendarName.text = item.calendar.name
            binding.calendarAccount.text = item.calendar.accountName
            binding.calendarCheckbox.setOnCheckedChangeListener(null)
            binding.calendarCheckbox.isChecked = item.isSelected
            binding.root.setOnClickListener {
                onCalendarToggled(item.calendar, !binding.calendarCheckbox.isChecked)
            }
            binding.calendarCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onCalendarToggled(item.calendar, isChecked)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CalendarSelectionItem>() {
        override fun areItemsTheSame(oldItem: CalendarSelectionItem, newItem: CalendarSelectionItem): Boolean {
            return oldItem.calendar.id == newItem.calendar.id
        }

        override fun areContentsTheSame(oldItem: CalendarSelectionItem, newItem: CalendarSelectionItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class CalendarSelectionItem(
    val calendar: DeviceCalendar,
    val isSelected: Boolean,
)
