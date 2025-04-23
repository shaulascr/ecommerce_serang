package com.alya.ecommerce_serang.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class SessionManager(context: Context) {
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val USER_TOKEN = "user_token"
        private const val USER_ID = "user_id"  // New constant for storing user ID

    }

    fun saveToken(token: String) {
        sharedPreferences.edit() {
            putString(USER_TOKEN, token)
        }
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(USER_TOKEN, null)
        Log.d("SessionManager", "Retrieved token: $token")
        return token
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit() {
            putString(USER_ID, userId)
        }
        Log.d("SessionManager", "Saved user ID: $userId")
    }

    fun getUserId(): String? {
        val userId = sharedPreferences.getString(USER_ID, null)
        Log.d("SessionManager", "Retrieved user ID: $userId")
        return userId
    }

    fun clearUserId() {
        sharedPreferences.edit() {
            remove(USER_ID)
        }
    }

    fun clearToken() {
        sharedPreferences.edit() {
            remove(USER_TOKEN)
        }
    }

    //clear data when log out
    fun clearAll() {
        sharedPreferences.edit() {
            clear()
        }
    }
}