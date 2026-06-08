package com.example.textlauncher.data

import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.DeviceCalendar
import java.util.Calendar

class CalendarRepository(context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun loadCalendars(): List<DeviceCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        return contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DeviceCalendar(
                            id = cursor.getLong(idIndex),
                            name = cursor.getString(nameIndex).orEmpty(),
                            accountName = cursor.getString(accountIndex).orEmpty(),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    fun loadUpcomingEvents(calendarIds: Set<Long>): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val end = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 2)
        }.timeInMillis
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(end.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
        )
        val selection = if (calendarIds.isEmpty()) {
            null
        } else {
            calendarIds.joinToString(
                prefix = "${CalendarContract.Instances.CALENDAR_ID} IN (",
                postfix = ")",
                separator = ",",
            ) { "?" }
        }
        val selectionArgs = calendarIds.takeIf { it.isNotEmpty() }?.map { it.toString() }?.toTypedArray()

        return contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val calendarIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarEvent(
                            id = cursor.getLong(idIndex),
                            title = cursor.getString(titleIndex).orEmpty().ifBlank { "Untitled" },
                            calendarName = cursor.getString(calendarIndex).orEmpty(),
                            startMillis = cursor.getLong(beginIndex),
                            endMillis = cursor.getLong(endIndex),
                            isAllDay = cursor.getInt(allDayIndex) == 1,
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
