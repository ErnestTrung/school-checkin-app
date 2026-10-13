package com.ernestnguyen.schoolcheckin.widget

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ernestnguyen.schoolcheckin.R
import com.ernestnguyen.schoolcheckin.portal.ScheduleEntry
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckInWidgetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val today = LocalDate.of(2026, 10, 13)
    private val lesson = sampleEntry()

    @Test
    fun showsCurrentClassWithKeywordAction() {
        val view = render(signedIn = true, entries = listOf(lesson), now = today.atTime(14, 0))
        assertEquals("Now", view.text(R.id.widget_label))
        assertEquals("Lập trình di động", view.text(R.id.widget_title))
        assertEquals("13:30 – 16:00 · P.301", view.text(R.id.widget_detail))
        assertEquals("Enter keyword", view.text(R.id.widget_action))
    }

    @Test
    fun showsNextClassBeforeItsWindowOpens() {
        val view = render(signedIn = true, entries = listOf(lesson), now = today.atTime(9, 0))
        assertEquals("Next", view.text(R.id.widget_label))
        assertEquals("Open schedule", view.text(R.id.widget_action))
    }

    @Test
    fun asksToSignInWithoutSession() {
        val view = render(signedIn = false, entries = null, now = today.atTime(14, 0))
        assertEquals("Sign in to check in", view.text(R.id.widget_title))
        assertEquals(View.GONE, view.findViewById<View>(R.id.widget_detail).visibility)
    }

    /** Inflates the widget the way a launcher would. */
    private fun render(signedIn: Boolean, entries: List<ScheduleEntry>?, now: LocalDateTime): View {
        lateinit var view: View
        instrumentation.runOnMainSync {
            view = CheckInWidget.buildViews(context, signedIn, entries, now).apply(context, FrameLayout(context))
        }
        return view
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun sampleEntry() = ScheduleEntry(
        id = "1",
        classSectionId = "L1",
        subject = "Lập trình di động",
        classSection = "LTDD.01",
        room = "P.301",
        lecturer = null,
        date = today,
        dateText = "13/10/2026",
        startHour = "13",
        startMinute = "30",
        endHour = "16",
        endMinute = "0",
        firstPeriod = "7",
        lastPeriod = "9",
        isExam = false,
        examShift = null,
        attendance = null,
    )
}
