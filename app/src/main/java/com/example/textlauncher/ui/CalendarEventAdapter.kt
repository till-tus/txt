package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.databinding.ItemCalendarEventBinding
import com.example.textlauncher.domain.CalendarEvent
import java.text.DateFormat
import java.util.Date

class CalendarEventAdapter(
    private val onEventClick: (CalendarEvent) -> Unit,
) : ListAdapter<CalendarEvent, CalendarEventAdapter.EventViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(
            binding = ItemCalendarEventBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEventClick = onEventClick,
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(
            event = getItem(position),
            showDayHeader = position == 0 || !isSameDay(getItem(position - 1), getItem(position)),
        )
    }

    private fun isSameDay(first: CalendarEvent, second: CalendarEvent): Boolean {
        return dayFormat.format(Date(first.startMillis)) == dayFormat.format(Date(second.startMillis))
    }

    class EventViewHolder(
        private val binding: ItemCalendarEventBinding,
        private val onEventClick: (CalendarEvent) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)

        fun bind(event: CalendarEvent, showDayHeader: Boolean) {
            binding.eventDayHeader.visibility = if (showDayHeader) View.VISIBLE else View.GONE
            binding.eventDayDivider.visibility = if (showDayHeader) View.VISIBLE else View.GONE
            binding.eventDayHeader.text = dateFormat.format(Date(event.startMillis))
            binding.eventTime.text = if (event.isAllDay) {
                "All day"
            } else {
                timeFormat.format(Date(event.startMillis))
            }
            binding.eventTitle.text = event.title
            binding.eventCalendar.text = event.calendarName
            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }
    }

    private companion object {
        val dayFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    }

    private object DiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id && oldItem.startMillis == newItem.startMillis
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
}
