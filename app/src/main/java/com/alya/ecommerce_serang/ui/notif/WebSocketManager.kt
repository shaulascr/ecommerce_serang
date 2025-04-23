package com.alya.ecommerce_serang.ui.notif

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "WebSocketManager"

    }

    fun startWebSocketConnection() {
        try {
            // Only start if we have a token
            if (sessionManager.getToken().isNullOrEmpty()) {
                Log.d(TAG, "No auth token available, not starting WebSocket service")
                return
            }

            Log.d(TAG, "Starting WebSocket service")
            val serviceIntent = Intent(context, SimpleWebSocketService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting WebSocket service: ${e.message}")
        }
    }

    fun stopWebSocketConnection() {
        try {
            Log.d(TAG, "Stopping WebSocket service")
            context.stopService(Intent(context, SimpleWebSocketService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WebSocket service: ${e.message}")
        }
    }
}