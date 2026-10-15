package com.ernestnguyen.schoolcheckin

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestnguyen.schoolcheckin.portal.PortalApi
import com.ernestnguyen.schoolcheckin.portal.ScheduleCache
import com.ernestnguyen.schoolcheckin.portal.SessionStore
import com.ernestnguyen.schoolcheckin.ui.LoginScreen
import com.ernestnguyen.schoolcheckin.ui.ScheduleScreen
import com.ernestnguyen.schoolcheckin.ui.ScheduleViewModel
import com.ernestnguyen.schoolcheckin.ui.SchoolCheckInTheme
import com.ernestnguyen.schoolcheckin.widget.CheckInWidget

class MainActivity : ComponentActivity() {
    /** Set when the home-screen widget opens the app to check in; cleared once the schedule screen acts on it. */
    private var checkInRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val store = SessionStore(this)
        val cache = ScheduleCache(applicationContext)
        val userAgent = WebSettings.getDefaultUserAgent(this)

        setContent {
            SchoolCheckInTheme {
                var session by remember { mutableStateOf(store.load()) }
                var expiredToken by rememberSaveable { mutableStateOf<String?>(null) }
                when (val current = session) {
                    null -> LoginScreen(
                        expiredToken = expiredToken,
                        onSignedIn = {
                            store.save(it)
                            expiredToken = null
                            session = it
                            CheckInWidget.refresh(applicationContext)
                        },
                    )
                    else -> {
                        val viewModel = viewModel(key = current.token) {
                            val api = PortalApi(current, userAgent) { url -> CookieManager.getInstance().getCookie(url) }
                            ScheduleViewModel(api, fallbackIp = current.portalIp) { weekStart, entries ->
                                cache.save(weekStart, entries)
                                CheckInWidget.refresh(applicationContext)
                            }
                        }
                        ScheduleScreen(
                            viewModel = viewModel,
                            checkInRequested = checkInRequested,
                            onCheckInRequestHandled = { checkInRequested = false },
                            onSignOut = {
                                store.clear()
                                cache.clear()
                                clearWebSession()
                                session = null
                                CheckInWidget.refresh(applicationContext)
                            },
                            // Keep cookies so Microsoft can sign straight back in.
                            onSessionExpired = {
                                store.clear()
                                expiredToken = current.token
                                session = null
                                CheckInWidget.refresh(applicationContext)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == CheckInWidget.ACTION_CHECK_IN_NOW) checkInRequested = true
    }
}

private fun clearWebSession() {
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
    WebStorage.getInstance().deleteAllData()
}
