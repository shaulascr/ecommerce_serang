package com.alya.ecommerce_serang.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application(){
    private val TAG = "AppSerang"

//    var tokenTes: String? = null

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Request FCM token at app startup
        retrieveFCMToken()
    }

    private fun retrieveFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
//                tokenTes = token
                Log.d(TAG, "FCM token retrieved: $token")

                // Save token locally
                val sharedPreferences = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("FCM_TOKEN", token).apply()

                // Send to your server
                sendTokenToServer(token)
            }
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "Would send token to server: $token")
    }
}