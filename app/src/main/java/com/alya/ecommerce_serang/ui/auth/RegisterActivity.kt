package com.alya.ecommerce_serang.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityRegisterBinding
import com.alya.ecommerce_serang.ui.MainActivity
import com.alya.ecommerce_serang.ui.auth.fragments.RegisterStep1Fragment
import com.alya.ecommerce_serang.ui.auth.fragments.RegisterStep2Fragment
import com.alya.ecommerce_serang.ui.auth.fragments.RegisterStep3Fragment
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager


    private val registerViewModel: RegisterViewModel by viewModels{
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val orderRepository = OrderRepository(apiService)
            val userRepository = UserRepository(apiService)
            RegisterViewModel(userRepository, orderRepository, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        Log.d("RegisterActivity", "Token in storage: '${sessionManager.getToken()}'")
        Log.d("RegisterActivity", "User ID in storage: '${sessionManager.getUserId()}'")

        try {
            // Use the new isLoggedIn method
            if (sessionManager.isLoggedIn()) {
                Log.d("RegisterActivity", "User logged in, redirecting to MainActivity")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            } else {
                Log.d("RegisterActivity", "User not logged in, showing RegisterActivity")
            }
        } catch (e: Exception) {
            // Handle any exceptions
            Log.e("RegisterActivity", "Error checking login status: ${e.message}", e)
            // Clear potentially corrupt data
            sessionManager.clearAll()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterStep1Fragment.newInstance())
                .commit()
        }
    }

    fun navigateToStep(step: Int, userData: RegisterRequest?) {
        val fragment = when (step) {
            1 -> RegisterStep1Fragment.newInstance()
            2 -> RegisterStep2Fragment.newInstance(userData)
            3 -> RegisterStep3Fragment.newInstance()
            else -> null
        }

        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, it)
                .addToBackStack(null)
                .commit()
        }
    }
}