package com.alya.ecommerce_serang.ui.notif.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alya.ecommerce_serang.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

    class FCMService : FirebaseMessagingService() {
        private val TAG = "FCMService"

        override fun onNewToken(token: String) {
            super.onNewToken(token)
            Log.d(TAG, "Refreshed FCM token: $token")

            // Store the token locally
            storeTokenLocally(token)

            // Send token to your server
            sendTokenToServer(token)
        }

        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            super.onMessageReceived(remoteMessage)
            Log.d(TAG, "From: ${remoteMessage.from}")

            // Handle data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: ${remoteMessage.data}")
                // Process data payload if needed
            }

            // Handle notification payload
            remoteMessage.notification?.let {
                Log.d(TAG, "Message notification: ${it.title} / ${it.body}")
                showNotification(it.title, it.body)
            }
        }

        private fun storeTokenLocally(token: String) {
            val sharedPreferences = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("FCM_TOKEN", token).apply()
        }

        private fun sendTokenToServer(token: String) {
            // TODO: Implement API call to your server to send the token
            // This is a placeholder - you'll need to replace with actual API call to your server
            Log.d(TAG, "Token would be sent to server: $token")
        }

        private fun showNotification(title: String?, body: String?) {
            val channelId = "fcm_default_channel"

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "FCM Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // Build notification
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.outline_notifications_24) // Make sure this resource exists
                .setContentTitle(title ?: "New Message")
                .setContentText(body ?: "You have a new notification")
                .setAutoCancel(true)

            // Show notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
