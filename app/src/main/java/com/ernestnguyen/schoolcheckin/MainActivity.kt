package com.ernestnguyen.schoolcheckin

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
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestnguyen.schoolcheckin.portal.PortalApi
import com.ernestnguyen.schoolcheckin.portal.SessionStore
import com.ernestnguyen.schoolcheckin.ui.LoginScreen
import com.ernestnguyen.schoolcheckin.ui.ScheduleScreen
import com.ernestnguyen.schoolcheckin.ui.ScheduleViewModel
import com.ernestnguyen.schoolcheckin.ui.SchoolCheckInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SessionStore(this)
        val userAgent = WebSettings.getDefaultUserAgent(this)

        setContent {
            SchoolCheckInTheme {
                var session by remember { mutableStateOf(store.load()) }
                when (val current = session) {
                    null -> LoginScreen(
                        onSignedIn = {
                            store.save(it)
                            session = it
                        },
                    )
                    else -> {
                        val viewModel = viewModel(key = current.token) {
                            val api = PortalApi(current, userAgent) { url -> CookieManager.getInstance().getCookie(url) }
                            ScheduleViewModel(api, fallbackIp = current.portalIp)
                        }
                        ScheduleScreen(
                            viewModel = viewModel,
                            onSignOut = {
                                store.clear()
                                clearWebSession()
                                session = null
                            },
                            // Keep cookies so Microsoft can sign straight back in.
                            onSessionExpired = {
                                store.clear()
                                session = null
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun clearWebSession() {
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
    WebStorage.getInstance().deleteAllData()
}
