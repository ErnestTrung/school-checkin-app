package com.ernestnguyen.schoolcheckin.portal

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/** The current week's schedule, kept for the home-screen widget, which runs without the app. */
class ScheduleCache(context: Context) {
    private val prefs = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)

    fun save(weekStart: LocalDate, entries: List<ScheduleEntry>) = prefs.edit {
        putString(KEY_WEEK_START, weekStart.toString())
        putString(KEY_ENTRIES, encodeSchedule(entries))
    }

    /** The stored entries if they are for the week starting [weekStart], else null. */
    fun load(weekStart: LocalDate): List<ScheduleEntry>? {
        if (prefs.getString(KEY_WEEK_START, null) != weekStart.toString()) return null
        return decodeSchedule(prefs.getString(KEY_ENTRIES, null) ?: return null)
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_WEEK_START = "week_start"
        const val KEY_ENTRIES = "entries"
    }
}
