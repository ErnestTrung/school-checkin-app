package com.ernestnguyen.schoolcheckin.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ernestnguyen.schoolcheckin.MainActivity
import com.ernestnguyen.schoolcheckin.R
import com.ernestnguyen.schoolcheckin.portal.EARLY_CHECK_IN_MINUTES
import com.ernestnguyen.schoolcheckin.portal.ScheduleCache
import com.ernestnguyen.schoolcheckin.portal.ScheduleEntry
import com.ernestnguyen.schoolcheckin.portal.SessionStore
import com.ernestnguyen.schoolcheckin.portal.currentClass
import com.ernestnguyen.schoolcheckin.portal.nextClass
import com.ernestnguyen.schoolcheckin.portal.startOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Home-screen shortcut for checking in: shows the class happening now and opens the app straight
 * into its check-in sheet. Widgets can't host text fields, so the keyword is typed in the app.
 */
class CheckInWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) refresh(context) else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    companion object {
        const val ACTION_CHECK_IN_NOW = "com.ernestnguyen.schoolcheckin.action.CHECK_IN_NOW"
        private const val ACTION_REFRESH = "com.ernestnguyen.schoolcheckin.action.REFRESH_WIDGET"

        /** Redraws every placed widget from the stored session and schedule. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CheckInWidget::class.java))
            if (ids.isEmpty()) return
            val now = LocalDateTime.now()
            val signedIn = SessionStore(context).load() != null
            val entries = if (signedIn) ScheduleCache(context).load(now.toLocalDate().startOfWeek()) else null
            manager.updateAppWidget(ids, buildViews(context, signedIn, entries, now))
            scheduleNextRefresh(context, entries, now)
        }

        internal fun buildViews(
            context: Context,
            signedIn: Boolean,
            entries: List<ScheduleEntry>?,
            now: LocalDateTime,
        ): RemoteViews {
            val current = entries?.let { currentClass(it, now) }
            val next = entries?.let { nextClass(it, now) }
            val content = when {
                !signedIn -> Content("CMC Check-in", "Sign in to check in", null, "Sign in", checkIn = false)
                current != null -> Content("Now", current.subject, detail(current), "Enter keyword", checkIn = true)
                // Nothing stored for this week yet: let the app load it and decide.
                entries == null -> Content("Attendance", "Schedule not loaded yet", null, "Open", checkIn = true)
                next != null -> Content("Next", next.subject, detail(next), "Open schedule", checkIn = false)
                else -> Content("Attendance", "No more classes today", null, "Open schedule", checkIn = false)
            }

            val intent = Intent(context, MainActivity::class.java)
            if (content.checkIn) intent.action = ACTION_CHECK_IN_NOW
            val open = PendingIntent.getActivity(
                context,
                if (content.checkIn) 1 else 0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            return RemoteViews(context.packageName, R.layout.widget_check_in).apply {
                setTextViewText(R.id.widget_label, content.label)
                setTextViewText(R.id.widget_title, content.title)
                setTextViewText(R.id.widget_detail, content.detail.orEmpty())
                setViewVisibility(R.id.widget_detail, if (content.detail == null) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_action, content.action)
                setOnClickPendingIntent(R.id.widget_root, open)
                setOnClickPendingIntent(R.id.widget_action, open)
            }
        }

        /**
         * Wakes the widget when today's next check-in window opens or closes, or at midnight.
         * The alarm is inexact and non-waking, so it needs no permission and costs no battery.
         */
        private fun scheduleNextRefresh(context: Context, entries: List<ScheduleEntry>?, now: LocalDateTime) {
            val today = now.toLocalDate()
            val minute = now.hour * 60 + now.minute
            val nextMinute = entries.orEmpty()
                .filter { !it.isExam && it.date == today }
                .flatMap { listOf(it.startMinutes - EARLY_CHECK_IN_MINUTES, it.endMinutes) }
                .filter { it > minute }
                .minOrNull()
            val at = nextMinute?.let { today.atStartOfDay().plusMinutes(it.toLong()) } ?: today.plusDays(1).atStartOfDay()
            context.getSystemService(AlarmManager::class.java).set(
                AlarmManager.RTC,
                at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                refreshIntent(context),
            )
        }

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, CheckInWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun detail(entry: ScheduleEntry) = listOfNotNull(entry.timeRange, entry.room).joinToString(" · ")
    }

    private class Content(
        val label: String,
        val title: String,
        val detail: String?,
        val action: String,
        /** Tapping asks the app to open check-in for the current class. */
        val checkIn: Boolean,
    )
}
