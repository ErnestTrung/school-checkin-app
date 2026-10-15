package com.ernestnguyen.schoolcheckin.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ernestnguyen.schoolcheckin.portal.PORTAL_ORIGIN
import com.ernestnguyen.schoolcheckin.portal.Session
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private const val LOGIN_URL = "$PORTAL_ORIGIN/congthongtin/login.aspx"

/** Where the portal's own 401 handler sends the browser (`edu.system.strlogouturl`); ends the portal session only. */
private const val LOGOUT_URL = "$PORTAL_ORIGIN/congthongtin/Logout.aspx"
private const val PROBE_INTERVAL_MS = 700L

/** How many probes to wait for the menu (and its "Lịch cá nhân" id) once a token exists. */
private const val MENU_WAIT_PROBES = 15

/**
 * Reads the session the portal's own `AFG()` puts on `edu.system` after Microsoft sign-in,
 * plus the id of the "Lịch cá nhân" entry from the menu list `getlistByUser_ChucNang` loads.
 */
private const val PROBE_JS = """
(function () {
  try {
    var s = (typeof edu !== 'undefined' && edu) ? edu.system : null;
    if (!s || !s.tokenJWT || !s.userId) return null;
    var list = s.dtChucNang || [], match = null;
    for (var i = 0; i < list.length; i++) {
      var file = String(list[i].DUONGDANFILE || '').toLowerCase();
      var name = String(list[i].TENCHUCNANG || '').toLowerCase();
      if (file.indexOf('lichhoc') >= 0 || name.indexOf('lịch cá nhân') >= 0) { match = list[i].ID; break; }
    }
    return JSON.stringify({
      token: s.tokenJWT, userId: s.userId, clientIP: s.clientIP || '',
      chucNangId: match, currentChucNangId: s.strChucNang_Id || '', menuLoaded: list.length > 0
    });
  } catch (e) { return null; }
})();
"""

private class Probe(
    val token: String,
    val userId: String,
    val clientIp: String,
    val chucNangId: String?,
    val currentChucNangId: String,
    val menuLoaded: Boolean,
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    /** Token the portal just rejected; never handed back as a new session. */
    expiredToken: String?,
    onSignedIn: (Session) -> Unit,
) {
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(topBar = { TopAppBar(title = { Text("Sign in with your school account") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(false)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val scheme = request.url.scheme
                                if (scheme == "http" || scheme == "https") return false
                                try {
                                    view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                } catch (e: ActivityNotFoundException) {
                                    // Nothing on the device handles this link; stay on the page.
                                }
                                return true
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                canGoBack = view.canGoBack()
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }

                        startSessionProbe(this, expiredToken) { session ->
                            CookieManager.getInstance().flush()
                            currentOnSignedIn(session)
                        }
                        loadUrl(LOGIN_URL)
                        webView = this
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.destroy()
                },
            )
            if (progress < 100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Polls the page until the portal has a signed-in session, then reports it once.
 *
 * The portal renders the token from its server session, which can outlive the token itself, so
 * after an expiry the page may still carry [expiredToken]. That one is skipped and the portal
 * session ended once, leaving the Microsoft sign-in in place to issue a fresh token.
 */
private fun startSessionProbe(webView: WebView, expiredToken: String?, onFound: (Session) -> Unit) {
    var probesWithToken = 0
    var loggedOut = false
    var done = false
    lateinit var tick: Runnable
    tick = Runnable {
        // Posted runnables only run once the view is attached; stop for good after it is released.
        if (done || !webView.isAttachedToWindow) return@Runnable
        webView.evaluateJavascript(PROBE_JS) { raw ->
            if (done) return@evaluateJavascript
            val probe = parseProbe(raw)
            when {
                probe == null -> webView.postDelayed(tick, PROBE_INTERVAL_MS)
                probe.token == expiredToken -> {
                    if (!loggedOut) {
                        loggedOut = true
                        webView.loadUrl(LOGOUT_URL)
                    }
                    webView.postDelayed(tick, PROBE_INTERVAL_MS)
                }
                (probe.menuLoaded && probe.chucNangId != null) || probesWithToken >= MENU_WAIT_PROBES -> {
                    done = true
                    onFound(
                        Session(
                            token = probe.token,
                            userId = probe.userId,
                            chucNangId = probe.chucNangId ?: probe.currentChucNangId,
                            portalIp = probe.clientIp,
                        )
                    )
                }
                else -> {
                    probesWithToken++
                    webView.postDelayed(tick, PROBE_INTERVAL_MS)
                }
            }
        }
    }
    webView.post(tick)
}

/** `evaluateJavascript` hands back the script's return value JSON-encoded, so the payload is a JSON string inside JSON. */
private fun parseProbe(raw: String?): Probe? {
    if (raw == null || raw == "null") return null
    return try {
        val payload = (Json.parseToJsonElement(raw) as? JsonPrimitive)?.contentOrNull ?: return null
        val obj = Json.parseToJsonElement(payload) as? JsonObject ?: return null
        fun field(key: String) = (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        Probe(
            token = field("token") ?: return null,
            userId = field("userId") ?: return null,
            clientIp = field("clientIP").orEmpty(),
            chucNangId = field("chucNangId"),
            currentChucNangId = field("currentChucNangId").orEmpty(),
            menuLoaded = (obj["menuLoaded"] as? JsonPrimitive)?.booleanOrNull == true,
        )
    } catch (e: SerializationException) {
        null
    }
}
