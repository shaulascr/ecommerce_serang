package com.alya.ecommerce_serang.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.alya.ecommerce_serang.data.api.dto.FcmReq
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityLoginBinding
import com.alya.ecommerce_serang.ui.MainActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.LoginViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels{
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val userRepository = UserRepository(apiService)
            LoginViewModel(userRepository, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
//            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(
//                systemBars.left,
//                systemBars.top,
//                systemBars.right,
//                systemBars.bottom
//            )
//            windowInsets
//        }

//        onBackPressedDispatcher.addCallback(this) {
//            // Handle the back button event
//        }

        setupListeners()
        observeLoginState()

        FirebaseApp.initializeApp(this)

        // Request FCM token at app startup
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString()
            val password = binding.etLoginPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginViewModel.login(email, password)
            }
        }

        binding.tvRegistrasi.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun observeLoginState() {
        loginViewModel.loginState.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    val accessToken = result.data.accessToken

                    val sessionManager = SessionManager(this)
                    sessionManager.saveToken(accessToken)
                    retrieveFCMToken()
//                    sessionManager.saveUserId(response.userId)

                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    Log.e("LoginActivity", "Login Failed: ${result.exception.message}")
                    Toast.makeText(this, "Login Failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                is Result.Loading -> {
                    // Show loading state
                }
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
        val tokenFcm=FcmReq(
            fcmToken = token
        )
        loginViewModel.sendFcm(tokenFcm)
        Log.d(TAG, "Sent token fcm: $token")

    }
}