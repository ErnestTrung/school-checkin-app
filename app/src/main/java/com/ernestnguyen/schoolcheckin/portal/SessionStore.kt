package com.ernestnguyen.schoolcheckin.portal

import android.content.Context
import androidx.core.content.edit

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun load(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return Session(
            token = token,
            userId = userId,
            chucNangId = prefs.getString(KEY_CHUC_NANG_ID, null).orEmpty(),
            portalIp = prefs.getString(KEY_PORTAL_IP, null).orEmpty(),
        )
    }

    fun save(session: Session) = prefs.edit {
        putString(KEY_TOKEN, session.token)
        putString(KEY_USER_ID, session.userId)
        putString(KEY_CHUC_NANG_ID, session.chucNangId)
        putString(KEY_PORTAL_IP, session.portalIp)
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_CHUC_NANG_ID = "chuc_nang_id"
        const val KEY_PORTAL_IP = "portal_ip"
    }
}
