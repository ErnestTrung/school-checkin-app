package com.ernestnguyen.schoolcheckin.portal

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentClassTest {
    private val today = LocalDate.of(2026, 10, 13)
    private val morning = entry("morning", start = "7:00", end = "9:30")
    private val afternoon = entry("afternoon", start = "13:30", end = "16:00")
    private val week = listOf(morning, afternoon)

    @Test
    fun picksClassUnderWay() {
        assertEquals("afternoon", currentClass(week, at("14:00"))?.id)
    }

    @Test
    fun picksClassStartingWithinEarlyWindow() {
        assertEquals("afternoon", currentClass(week, at("13:15"))?.id)
        assertNull(currentClass(week, at("13:14")))
    }

    @Test
    fun endTimeIsExclusive() {
        assertEquals("morning", currentClass(week, at("9:29"))?.id)
        assertNull(currentClass(week, at("9:30")))
    }

    @Test
    fun ignoresOtherDaysAndExams() {
        val tomorrow = entry("tomorrow", start = "13:30", end = "16:00", date = today.plusDays(1))
        val exam = entry("exam", start = "13:30", end = "15:00", isExam = true)
        assertNull(currentClass(listOf(tomorrow, exam), at("14:00")))
    }

    @Test
    fun overlappingClassesPreferTheLaterStart() {
        val next = entry("next", start = "15:50", end = "18:00")
        assertEquals("next", currentClass(week + next, at("15:55"))?.id)
    }

    @Test
    fun nextClassIsTheFirstWhoseWindowHasNotOpened() {
        assertEquals("morning", nextClass(week, at("6:00"))?.id)
        assertEquals("afternoon", nextClass(week, at("10:00"))?.id)
        assertNull(nextClass(week, at("13:15")))
    }

    @Test
    fun storedWeekReadsBackUnchanged() {
        val exam = entry("exam", start = "07:30", end = "9:00", isExam = true).copy(examShift = "Ca 1", room = "P.301")
        val stored = week + exam
        assertEquals(stored.sortedWith(compareBy({ it.date }, { it.startMinutes })), decodeSchedule(encodeSchedule(stored)))
    }

    private fun at(time: String): LocalDateTime {
        val (hour, minute) = time.split(":").map { it.toInt() }
        return today.atTime(hour, minute)
    }

    private fun entry(
        id: String,
        start: String,
        end: String,
        date: LocalDate = today,
        isExam: Boolean = false,
    ): ScheduleEntry {
        val (startHour, startMinute) = start.split(":")
        val (endHour, endMinute) = end.split(":")
        return ScheduleEntry(
            id = id,
            classSectionId = "L-$id",
            subject = id,
            classSection = null,
            room = null,
            lecturer = null,
            date = date,
            dateText = date.format(PORTAL_DATE),
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            firstPeriod = null,
            lastPeriod = null,
            isExam = isExam,
            examShift = null,
            attendance = null,
        )
    }
}
