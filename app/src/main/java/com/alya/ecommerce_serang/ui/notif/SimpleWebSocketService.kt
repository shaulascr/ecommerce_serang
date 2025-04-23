package com.alya.ecommerce_serang.ui.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alya.ecommerce_serang.BuildConfig
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class SimpleWebSocketService : Service() {

    companion object {
        private const val TAG = "SocketIOService"
        private const val NOTIFICATION_CHANNEL_ID = "websocket_service_channel"
        private const val FOREGROUND_SERVICE_ID = 1001
    }

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
            .setPriority(NotificationCompat.PRIORITY_MIN) // Set the lowest priority
            .setSound(null) // No sound
            .setVibrate(longArrayOf(0L)) // No vibration
            .setContentText("") // Empty text or minimal text
            .setOngoing(true) // Keeps it ongoing
            .build()

        startForeground(1, notification)


        startForeground(FOREGROUND_SERVICE_ID, notification)
        serviceScope.launch { initSocket() }
        return START_STICKY
    }

    private suspend fun initSocket() {
        val userId = sessionManager.getUserId() ?: run {
            Log.e(TAG, "User ID not available")
            stopSelf()
            return
        }

        val options = IO.Options().apply {
            forceNew = true
            reconnection = true
            reconnectionDelay = 1000 // Retry every 1 second if disconnected
            reconnectionAttempts = Int.MAX_VALUE
        }

        socket = IO.socket(BuildConfig.BASE_URL, options)
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket.IO connected")
                emit("joinRoom", userId)
            }

            on("notification") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as? JSONObject
                    val title = data?.optString("title", "New Notification") ?: "Notification"
                    val message = data?.optString("message", "") ?: ""
                    showNotification(title, message)
                }
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket.IO disconnected")
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket.IO connection error: ${args.firstOrNull()}")
            }

            connect()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notification = notificationBuilder
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } else {
                Log.e(TAG, "Notification permission not granted")
            }
        } else {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "WebSocket Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for WebSocket Service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        socket?.disconnect()
        socket?.off()
        serviceScope.cancel()
        super.onDestroy()
    }
}