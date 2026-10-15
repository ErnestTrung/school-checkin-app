package com.ernestnguyen.schoolcheckin.portal

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal const val PORTAL_ORIGIN = "https://iu.cmcu.edu.vn"

/** The page whose scripts make the portal's API calls. */
private const val PORTAL_PAGE = "$PORTAL_ORIGIN/congthongtin/Index.aspx"

/** The part of Config.js `Init_API()` the app uses: action prefix (text before the first '_') → API base path. */
private val API_BASES = mapOf(
    "SV" to "/sinhvienapi/api",
    "CC" to "/chuyencanapi/api",
)

class SessionExpiredException : IOException("Session expired")

class PortalException(message: String) : Exception(message)

internal class PortalResponse(val data: JsonElement?, val message: String?)

/**
 * Calls the student portal the way `edu.system.makeRequest` (systemroot.js) does: a form POST to
 * `<origin><API base>/<action>` carrying the sign-in JWT as a Bearer token and the portal's cookies.
 */
class PortalApi(
    private val session: Session,
    private val userAgent: String,
    private val cookies: (url: String) -> String?,
) {
    suspend fun weekSchedule(from: LocalDate, to: LocalDate): List<ScheduleEntry> {
        val response = callEncrypted(
            action = "SV_ThongTin_MH/DSA4BRINKCIpAiAPKSAv",
            func = "pkg_congthongtin_hssv_thongtin.LayDSLichCaNhan",
            params = listOf(
                "strQLSV_NguoiHoc_Id" to session.userId,
                "strNgayBatDau" to from.format(PORTAL_DATE),
                "strNgayKetThuc" to to.format(PORTAL_DATE),
            ),
        )
        return parseSchedule(response.data)
    }

    /**
     * Submits the attendance keyword with the same fields, in the same order, as the portal's
     * `save_TuGhiNhan` (lichgiang.js). That request carries no `iM`, so it is not encrypted.
     * Returns the portal's message.
     */
    suspend fun checkIn(entry: ScheduleEntry, keyword: String, ip: String): String {
        val action = "CC_ThongTin/Them_QLSV_NguoiHoc_TuGhiNhan"
        val response = parseResponse(
            post(
                action,
                listOf(
                    "action" to action,
                    "type" to "POST",
                    "strChucNang_Id" to session.chucNangId,
                    "strDaoTao_LopQuanLy_Id" to "",
                    "strDiem_DanhSach_Id" to entry.classSectionId,
                    "strNgayGhiNhan" to entry.dateText,
                    "strGio" to entry.startHour,
                    "strPhut" to entry.startMinute,
                    "strGiay" to "0",
                    "strIp" to ip,
                    "strNoiDungTuGhiNhan" to keyword,
                    "strNguoiThucHien_Id" to session.userId,
                    "strQLSV_NguoiHoc_Id" to session.userId,
                    "strDaoTao_ChuongTrinh_Id" to "",
                    "strQLSV_TrangThaiNguoiHoc_Id" to "",
                ),
            )
        )
        return response.message ?: "Checked in."
    }

    private suspend fun callEncrypted(
        action: String,
        func: String,
        params: List<Pair<String, String>>,
    ): PortalResponse {
        val payload = buildJsonObject {
            put("action", action)
            put("func", func)
            put("iM", PortalCrypto.IM)
            params.forEach { (key, value) -> put(key, value) }
            put("strChucNang_Id", session.chucNangId)
            put("strNguoiThucHien_Id", session.userId)
        }
        val encrypted = PortalCrypto.encrypt(payload.toString(), action.substringAfter('/'))
        return parseResponse(post(action, listOf("A" to encrypted)))
    }

    private suspend fun post(action: String, fields: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val url = apiUrl(action)
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                requestHeaders(session.token, userAgent, cookies(url), Locale.getDefault())
                    .forEach { (name, value) -> connection.setRequestProperty(name, value) }

                val body = fields.joinToString("&") { (key, value) ->
                    URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
                }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_UNAUTHORIZED) throw SessionExpiredException()
                if (status !in 200..299) throw IOException("The portal answered HTTP $status")
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
}

/**
 * Headers for an API call. The API answers 401, even with a valid token, unless the request also
 * carries an `Accept-Language` and a `Referer` from a portal page, as the portal's own XHRs do.
 */
internal fun requestHeaders(token: String, userAgent: String, cookie: String?, locale: Locale): List<Pair<String, String>> =
    listOfNotNull(
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "Accept-Language" to locale.toLanguageTag(),
        "Authorization" to "Bearer $token",
        "User-Agent" to userAgent,
        "Origin" to PORTAL_ORIGIN,
        "Referer" to PORTAL_PAGE,
        cookie?.let { "Cookie" to it },
    )

internal fun apiUrl(action: String): String {
    val base = API_BASES[action.substringBefore('_')]
        ?: throw IllegalArgumentException("No API base configured for $action")
    return "$PORTAL_ORIGIN$base/$action"
}

/** Mirrors makeRequest's success handler: `{Success, Message, Data}`, where `Data` may be `{B: <AE ciphertext>}`. */
internal fun parseResponse(body: String): PortalResponse {
    val root = try {
        Json.parseToJsonElement(body) as? JsonObject
    } catch (e: SerializationException) {
        null
    } ?: throw PortalException("Unexpected response from the portal.")

    val message = (root["Message"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    if ((root["Success"] as? JsonPrimitive)?.booleanOrNull != true) {
        throw PortalException(message ?: "The portal rejected the request.")
    }
    val data = root["Data"]
    val encrypted = ((data as? JsonObject)?.get("B") as? JsonPrimitive)?.contentOrNull
    val decoded = if (encrypted != null) {
        try {
            Json.parseToJsonElement(PortalCrypto.decrypt(encrypted, PortalCrypto.IM))
        } catch (e: IllegalArgumentException) {
            // Bad Base64 or JSON (SerializationException is an IllegalArgumentException).
            throw PortalException("Couldn't read the portal's response.")
        }
    } else {
        data
    }
    return PortalResponse(decoded, message)
}

/** The portal fills `clientIP` from jsonip.com; check-in reports the same lookup. */
object PublicIp {
    suspend fun lookup(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://jsonip.com").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                ((Json.parseToJsonElement(text) as? JsonObject)?.get("ip") as? JsonPrimitive)
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }
}
