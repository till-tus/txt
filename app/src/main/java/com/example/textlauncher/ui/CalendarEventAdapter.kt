package com.example.textlauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.R
import com.example.textlauncher.databinding.ItemCalendarEventBinding
import com.example.textlauncher.domain.CalendarEvent
import java.text.DateFormat
import java.util.Calendar
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
        val firstDay = Calendar.getInstance().apply { timeInMillis = first.startMillis }
        val secondDay = Calendar.getInstance().apply { timeInMillis = second.startMillis }
        return firstDay.get(Calendar.ERA) == secondDay.get(Calendar.ERA) &&
            firstDay.get(Calendar.YEAR) == secondDay.get(Calendar.YEAR) &&
            firstDay.get(Calendar.DAY_OF_YEAR) == secondDay.get(Calendar.DAY_OF_YEAR)
    }

    class EventViewHolder(
        private val binding: ItemCalendarEventBinding,
        private val onEventClick: (CalendarEvent) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: CalendarEvent, showDayHeader: Boolean) {
            val context = binding.root.context
            val locale = context.resources.configuration.locales[0]
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
            binding.eventDayHeader.visibility = if (showDayHeader) View.VISIBLE else View.GONE
            binding.eventDayDivider.visibility = if (showDayHeader) View.VISIBLE else View.GONE
            binding.eventDayHeader.text = dateFormat.format(Date(event.startMillis))
            binding.eventTime.text = if (event.isAllDay) {
                context.getString(R.string.calendar_event_all_day)
            } else {
                timeFormat.format(Date(event.startMillis))
            }
            binding.eventTitle.text = event.title.ifBlank {
                context.getString(R.string.calendar_event_untitled)
            }
            binding.eventCalendar.text = event.calendarName
            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }
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
