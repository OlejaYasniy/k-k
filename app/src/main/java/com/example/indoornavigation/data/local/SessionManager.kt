package com.example.indoornavigation.data.local

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var userId: Int
        get() = prefs.getInt("user_id", -1)
        set(v) = prefs.edit().putInt("user_id", v).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(v) = prefs.edit().putString("username", v).apply()

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(v) = prefs.edit().putString("email", v).apply()

    val isLoggedIn get() = userId != -1

    fun clear() = prefs.edit().clear().apply()
}