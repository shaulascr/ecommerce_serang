package com.alya.ecommerce_serang.ui.notif

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityNotificationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint // Required for Hilt
class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val viewModel: NotifViewModel by viewModels()

    // Permission request code
    private val NOTIFICATION_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { result ->
                    when (result) {
                        is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                            // User profile loaded successfully
                            // Potentially do something with user profile
                        }
                        is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                            // Handle error - show message, etc.
                            Toast.makeText(this@NotificationActivity,
                                "Failed to load profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Result.Loading -> {
                            // Show loading indicator if needed
                        }
                    }
                }
            }
        }

        // Start WebSocket connection
//        viewModel.startWebSocketConnection()

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check and request notification permission for Android 13+
        requestNotificationPermissionIfNeeded()

        // Set up button click listeners
//        setupButtonListeners()


    }

//    private fun setupButtonListeners() {
//        binding.simpleNotification.setOnClickListener {
//            viewModel.showSimpleNotification()
//        }
//
//        binding.updateNotification.setOnClickListener {
//            viewModel.updateSimpleNotification()
//        }
//
//        binding.cancelNotification.setOnClickListener {
//            viewModel.cancelSimpleNotification()
//        }
//    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                // You might want to show a dialog explaining why notifications are important
            }
        }
    }
}