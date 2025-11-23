package com.example.getjob.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ServiFastPrefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_PROFILE_CREATED_FIRST_TIME = "profile_created_first_time"
    }
    
    fun saveAuthData(token: String, userId: Int, email: String, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply() // Usar apply() para operación asíncrona y no bloquear el hilo principal
        }
    }
    
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    
    fun clearAuthData() {
        prefs.edit().clear().apply() // Usar apply() para operación asíncrona
    }
    
    fun setProfileCreatedFirstTime(value: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_CREATED_FIRST_TIME, value).apply()
    }
    
    fun isProfileCreatedFirstTime(): Boolean = prefs.getBoolean(KEY_PROFILE_CREATED_FIRST_TIME, false)
}

