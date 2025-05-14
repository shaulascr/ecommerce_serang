package com.alya.ecommerce_serang.ui.notif.fcm

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"

    fun getToken(callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                    callback(null)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM token retrieved: $token")
                callback(token)
            }
    }

    fun getStoredToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
        return sharedPreferences.getString("FCM_TOKEN", null)
    }
}