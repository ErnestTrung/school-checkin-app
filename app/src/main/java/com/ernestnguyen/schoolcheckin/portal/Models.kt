package com.ernestnguyen.schoolcheckin.portal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** What the app keeps from the portal's `edu.system` after sign-in. */
data class Session(
    val token: String,
    val userId: String,
    /** `strChucNang_Id` of the "Lịch cá nhân" menu entry; the portal sends it with every call from that page. */
    val chucNangId: String,
    /** IP the portal recorded at sign-in; used only when a fresh lookup fails. */
    val portalIp: String,
)

/** One row of `LayDSLichCaNhan`. Raw strings are kept where check-in echoes them back. */
data class ScheduleEntry(
    val id: String,
    /** `IDLOPHOCPHAN`, sent as `strDiem_DanhSach_Id` on check-in. */
    val classSectionId: String,
    val subject: String,
    val classSection: String?,
    val room: String?,
    val lecturer: String?,
    val date: LocalDate,
    /** `NGAYHOC` exactly as the portal returned it. */
    val dateText: String,
    val startHour: String,
    val startMinute: String,
    val endHour: String,
    val endMinute: String,
    val firstPeriod: String?,
    val lastPeriod: String?,
    val isExam: Boolean,
    val examShift: String?,
    /** `THONGTINCHUYENCAN`, the attendance note the portal shows next to the session. */
    val attendance: String?,
) {
    val startTime: String get() = clock(startHour, startMinute)

    val timeRange: String get() = "$startTime – ${clock(endHour, endMinute)}"

    internal val startMinutes: Int
        get() = (startHour.toIntOrNull() ?: 0) * 60 + (startMinute.toIntOrNull() ?: 0)

    internal val endMinutes: Int
        get() = (endHour.toIntOrNull() ?: 0) * 60 + (endMinute.toIntOrNull() ?: 0)
}

/** How long before a class starts it already counts as the current one. */
internal const val EARLY_CHECK_IN_MINUTES = 15

/**
 * The class to check in to at [now]: today's non-exam session that is under way or starts within
 * [EARLY_CHECK_IN_MINUTES]. When sessions overlap, the one that started last wins.
 */
internal fun currentClass(entries: List<ScheduleEntry>, now: LocalDateTime): ScheduleEntry? {
    val minute = now.hour * 60 + now.minute
    return entries
        .filter { !it.isExam && it.date == now.toLocalDate() }
        .filter { it.startMinutes - EARLY_CHECK_IN_MINUTES <= minute && minute < it.endMinutes }
        .maxByOrNull { it.startMinutes }
}

/** Today's next class whose check-in window hasn't opened yet (see [currentClass]). */
internal fun nextClass(entries: List<ScheduleEntry>, now: LocalDateTime): ScheduleEntry? {
    val minute = now.hour * 60 + now.minute
    return entries
        .filter { !it.isExam && it.date == now.toLocalDate() && it.startMinutes - EARLY_CHECK_IN_MINUTES > minute }
        .minByOrNull { it.startMinutes }
}

internal fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal fun encodeSchedule(entries: List<ScheduleEntry>): String =
    JsonArray(entries.map { it.toPortalJson() }).toString()

internal fun decodeSchedule(text: String): List<ScheduleEntry> = try {
    parseSchedule(Json.parseToJsonElement(text))
} catch (e: SerializationException) {
    emptyList()
}

/** Back to the portal's row shape, so a stored week reads in through [parseSchedule] unchanged. */
private fun ScheduleEntry.toPortalJson(): JsonObject = buildJsonObject {
    put("ID", id)
    put("IDLOPHOCPHAN", classSectionId)
    put("TENHOCPHAN", subject)
    put("TENLOPHOCPHAN", classSection)
    put("TENPHONGHOC", room)
    put("GIANGVIEN", lecturer)
    put("NGAYHOC", dateText)
    put("GIOBATDAU", startHour)
    put("PHUTBATDAU", startMinute)
    put("GIOKETTHUC", endHour)
    put("PHUTKETTHUC", endMinute)
    put("TIETBATDAU", firstPeriod)
    put("TIETKETTHUC", lastPeriod)
    put("PHANLOAI", if (isExam) "LICHTHI" else null)
    put("CATHI", examShift)
    put("THONGTINCHUYENCAN", attendance)
}

internal val PORTAL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val LENIENT_DATE = DateTimeFormatter.ofPattern("d/M/yyyy")

internal fun parseSchedule(data: JsonElement?): List<ScheduleEntry> =
    (data as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonObject)?.toScheduleEntry() }
        .sortedWith(compareBy({ it.date }, { it.startMinutes }))

private fun JsonObject.toScheduleEntry(): ScheduleEntry? {
    val id = text("ID") ?: return null
    val dateText = text("NGAYHOC") ?: return null
    val date = try {
        LocalDate.parse(dateText, LENIENT_DATE)
    } catch (e: DateTimeParseException) {
        return null
    }
    return ScheduleEntry(
        id = id,
        classSectionId = text("IDLOPHOCPHAN").orEmpty(),
        subject = text("TENHOCPHAN") ?: text("DANGKY_LOPHOCPHAN_TEN") ?: "—",
        classSection = text("TENLOPHOCPHAN"),
        room = text("TENPHONGHOC") ?: text("PHONGHOC_TEN"),
        lecturer = text("GIANGVIEN"),
        date = date,
        dateText = dateText,
        startHour = text("GIOBATDAU") ?: "0",
        startMinute = text("PHUTBATDAU") ?: "0",
        endHour = text("GIOKETTHUC") ?: "0",
        endMinute = text("PHUTKETTHUC") ?: "0",
        firstPeriod = text("TIETBATDAU"),
        lastPeriod = text("TIETKETTHUC"),
        isExam = text("PHANLOAI") == "LICHTHI",
        examShift = text("CATHI"),
        attendance = text("THONGTINCHUYENCAN"),
    )
}

/** Field value as the portal's JavaScript would stringify it (numbers lose a trailing ".0"). */
private fun JsonObject.text(key: String): String? {
    val value = this[key] as? JsonPrimitive ?: return null
    if (value is JsonNull) return null
    val text = if (value.isString) {
        value.content
    } else {
        val number = value.content.toDoubleOrNull()
        when {
            number == null -> value.content
            number % 1.0 == 0.0 -> number.toLong().toString()
            else -> number.toString()
        }
    }
    return text.trim().ifEmpty { null }
}

private fun clock(hour: String, minute: String) = "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}"
