package com.ernestnguyen.schoolcheckin.portal

import java.time.LocalDate
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalApiTest {
    @Test
    fun apiUrlUsesInitApiBases() {
        assertEquals(
            "https://iu.cmcu.edu.vn/sinhvienapi/api/SV_ThongTin_MH/DSA4BRINKCIpAiAPKSAv",
            apiUrl("SV_ThongTin_MH/DSA4BRINKCIpAiAPKSAv"),
        )
        assertEquals(
            "https://iu.cmcu.edu.vn/chuyencanapi/api/CC_ThongTin/Them_QLSV_NguoiHoc_TuGhiNhan",
            apiUrl("CC_ThongTin/Them_QLSV_NguoiHoc_TuGhiNhan"),
        )
    }

    @Test
    fun requestHeadersCarryWhatTheApiRequires() {
        val headers = requestHeaders("tok", "UA", ".Auth=x", Locale.forLanguageTag("vi-VN")).toMap()
        assertEquals("Bearer tok", headers["Authorization"])
        assertEquals("UA", headers["User-Agent"])
        assertEquals(".Auth=x", headers["Cookie"])
        assertEquals("vi-VN", headers["Accept-Language"])
        assertEquals("https://iu.cmcu.edu.vn/congthongtin/Index.aspx", headers["Referer"])
    }

    @Test
    fun requestHeadersOmitCookieWhenThereIsNone() {
        assertFalse(requestHeaders("tok", "UA", null, Locale.US).any { it.first == "Cookie" })
    }

    @Test
    fun parseResponseDecryptsDataB() {
        val response = parseResponse("""{"Success":true,"Message":"","Data":{"B":"$RESPONSE_ENCRYPTED"}}""")
        val row = (response.data as JsonArray)[0] as JsonObject
        assertEquals("Lập trình di động", row["TENHOCPHAN"]!!.jsonPrimitive.content)
        assertNull(response.message)
    }

    @Test
    fun parseResponseSurfacesPortalMessageOnFailure() {
        val error = assertThrows(PortalException::class.java) {
            parseResponse("""{"Success":false,"Message":"Từ khóa không đúng","Data":null}""")
        }
        assertEquals("Từ khóa không đúng", error.message)
    }

    @Test
    fun parseResponseRejectsNonJson() {
        assertThrows(PortalException::class.java) { parseResponse("<html>Login</html>") }
    }

    @Test
    fun parseScheduleKeepsPortalValuesAndSortsByTime() {
        val rows = Json.parseToJsonElement(
            """
            [
              {"ID":"b","IDLOPHOCPHAN":"L1","TENHOCPHAN":"Mạng máy tính","TENLOPHOCPHAN":"MMT.01","NGAYHOC":"13/10/2026",
               "GIOBATDAU":13,"PHUTBATDAU":30,"GIOKETTHUC":16,"PHUTKETTHUC":0,"TIETBATDAU":7,"TIETKETTHUC":9,
               "TENPHONGHOC":"P.301","GIANGVIEN":null,"PHANLOAI":null,"THONGTINCHUYENCAN":"Có mặt"},
              {"ID":"a","IDLOPHOCPHAN":"L2","TENHOCPHAN":"Toán rời rạc","NGAYHOC":"13/10/2026",
               "GIOBATDAU":"07","PHUTBATDAU":"0","GIOKETTHUC":9.0,"PHUTKETTHUC":30,"PHANLOAI":"LICHTHI","CATHI":"Ca 1"},
              {"TENHOCPHAN":"row without ID is skipped","NGAYHOC":"13/10/2026"}
            ]
            """
        )

        val entries = parseSchedule(rows)

        assertEquals(listOf("a", "b"), entries.map { it.id })
        val exam = entries[0]
        assertTrue(exam.isExam)
        assertEquals("07", exam.startHour)
        assertEquals("9", exam.endHour)
        assertEquals("07:00 – 09:30", exam.timeRange)
        assertEquals("Ca 1", exam.examShift)

        val lesson = entries[1]
        assertFalse(lesson.isExam)
        assertEquals(LocalDate.of(2026, 10, 13), lesson.date)
        assertEquals("13/10/2026", lesson.dateText)
        assertEquals("L1", lesson.classSectionId)
        assertEquals("13", lesson.startHour)
        assertEquals("30", lesson.startMinute)
        assertEquals("P.301", lesson.room)
        assertNull(lesson.lecturer)
        assertEquals("7", lesson.firstPeriod)
        assertEquals("Có mặt", lesson.attendance)
    }
}
