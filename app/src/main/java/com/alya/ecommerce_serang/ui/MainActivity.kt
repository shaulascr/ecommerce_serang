package com.alya.ecommerce_serang.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.databinding.ActivityMainBinding
import com.alya.ecommerce_serang.ui.notif.WebSocketManager
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

//    private val viewModel: NotifViewModel by viewModels()
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    }

    companion object{
        private const val NOTIFICATION_PERMISSION_CODE = 100
    }

    @Inject
    lateinit var webSocketManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            windowInsets
        }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Request FCM token at app startup
        retrieveFCMToken()

        requestNotificationPermissionIfNeeded()

        // Start WebSocket service through WebSocketManager after permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                webSocketManager.startWebSocketConnection()
            }
        } else {
            webSocketManager.startWebSocketConnection()
        }

        setupBottomNavigation()
        observeDestinationChanges()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            webSocketManager.stopWebSocketConnection()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_item_fragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.chat_item_fragment -> {
                    navController.navigate(R.id.chatFragment)
                    true
                }
                R.id.profile_item_fragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeDestinationChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.homeFragment, R.id.chatFragment, R.id.profileFragment -> true
                else -> false
            }
        }
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                webSocketManager.startWebSocketConnection()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
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