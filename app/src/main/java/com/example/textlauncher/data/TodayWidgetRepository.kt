package com.example.textlauncher.data

import android.content.Context
import androidx.core.content.edit
import com.example.textlauncher.domain.TodayWidget
import com.example.textlauncher.domain.TodayWidgetType
import org.json.JSONArray
import org.json.JSONObject

class TodayWidgetRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadWidgets(): List<TodayWidget> {
        val stored = preferences.getString(KEY_WIDGETS, null) ?: return defaultWidgets()
        return runCatching {
            val widgets = JSONArray(stored)
            List(widgets.length()) { index ->
                val widget = widgets.getJSONObject(index)
                TodayWidget(
                    id = widget.getString(FIELD_ID),
                    type = TodayWidgetType.valueOf(widget.getString(FIELD_TYPE)),
                    column = widget.getInt(FIELD_COLUMN),
                    row = widget.getInt(FIELD_ROW),
                    columnSpan = widget.getInt(FIELD_COLUMN_SPAN),
                    rowSpan = widget.getInt(FIELD_ROW_SPAN),
                    notificationAppPackageNames = widget.optJSONArray(FIELD_NOTIFICATION_APP_PACKAGE_NAMES)
                        ?.toStringSet()
                        .orEmpty(),
                ).coerceToGrid()
            }
        }.getOrElse { defaultWidgets() }
    }

    fun saveWidgets(widgets: List<TodayWidget>) {
        val payload = JSONArray()
        widgets.forEach { widget ->
            val coerced = widget.coerceToGrid()
            payload.put(
                JSONObject()
                    .put(FIELD_ID, coerced.id)
                    .put(FIELD_TYPE, coerced.type.name)
                    .put(FIELD_COLUMN, coerced.column)
                    .put(FIELD_ROW, coerced.row)
                    .put(FIELD_COLUMN_SPAN, coerced.columnSpan)
                    .put(FIELD_ROW_SPAN, coerced.rowSpan)
                    .put(
                        FIELD_NOTIFICATION_APP_PACKAGE_NAMES,
                        JSONArray(coerced.notificationAppPackageNames.sorted()),
                    ),
            )
        }
        preferences.edit {
            putString(KEY_WIDGETS, payload.toString())
        }
    }

    private fun defaultWidgets(): List<TodayWidget> {
        return listOf(defaultNextEventWidget())
    }

    fun defaultNextEventWidget(): TodayWidget {
        return TodayWidget(
            id = NEXT_EVENT_WIDGET_ID,
            type = TodayWidgetType.NextEvent,
            column = 0,
            row = 0,
            columnSpan = MIN_COLUMN_SPAN,
            rowSpan = MIN_ROW_SPAN,
        )
    }

    fun defaultNotificationFeedWidget(): TodayWidget {
        return TodayWidget(
            id = NOTIFICATION_FEED_WIDGET_ID,
            type = TodayWidgetType.NotificationFeed,
            column = 0,
            row = 0,
            columnSpan = MIN_COLUMN_SPAN,
            rowSpan = MIN_ROW_SPAN,
        )
    }

    fun defaultWeatherWidget(): TodayWidget {
        return TodayWidget(
            id = WEATHER_WIDGET_ID,
            type = TodayWidgetType.Weather,
            column = 0,
            row = 0,
            columnSpan = MIN_COLUMN_SPAN,
            rowSpan = MIN_ROW_SPAN,
        )
    }

    fun defaultPinnedNoteWidget(): TodayWidget {
        return TodayWidget(
            id = PINNED_NOTE_WIDGET_ID,
            type = TodayWidgetType.PinnedNote,
            column = 0,
            row = 0,
            columnSpan = MIN_COLUMN_SPAN,
            rowSpan = MIN_ROW_SPAN,
        )
    }

    private fun TodayWidget.coerceToGrid(): TodayWidget {
        val coercedColumnSpan = columnSpan.coerceIn(MIN_COLUMN_SPAN, GRID_COLUMNS)
        val coercedRowSpan = rowSpan.coerceIn(MIN_ROW_SPAN, GRID_ROWS)
        return copy(
            columnSpan = coercedColumnSpan,
            rowSpan = coercedRowSpan,
            column = column.coerceIn(0, GRID_COLUMNS - coercedColumnSpan),
            row = row.coerceIn(0, GRID_ROWS - coercedRowSpan),
        )
    }

    private fun JSONArray.toStringSet(): Set<String> {
        return List(length()) { index -> optString(index) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private companion object {
        const val PREFERENCES_NAME = "today_widgets"
        const val KEY_WIDGETS = "widgets"
        const val FIELD_ID = "id"
        const val FIELD_TYPE = "type"
        const val FIELD_COLUMN = "column"
        const val FIELD_ROW = "row"
        const val FIELD_COLUMN_SPAN = "columnSpan"
        const val FIELD_ROW_SPAN = "rowSpan"
        const val FIELD_NOTIFICATION_APP_PACKAGE_NAMES = "notificationAppPackageNames"
        const val NEXT_EVENT_WIDGET_ID = "next_event"
        const val WEATHER_WIDGET_ID = "weather"
        const val NOTIFICATION_FEED_WIDGET_ID = "notification_feed"
        const val PINNED_NOTE_WIDGET_ID = "pinned_note"
        const val GRID_COLUMNS = 4
        const val GRID_ROWS = 6
        const val MIN_COLUMN_SPAN = 2
        const val MIN_ROW_SPAN = 1
    }
}
