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

    // In RegisterActivity, add debug to navigateToStep:

    fun navigateToStep(step: Int, userData: RegisterRequest?) {
        Log.d("RegisterActivity", "=== NAVIGATE TO STEP START ===")
        Log.d("RegisterActivity", "Target step: $step")
        Log.d("RegisterActivity", "Current fragment count: ${supportFragmentManager.fragments.size}")
        Log.d("RegisterActivity", "UserData: ${userData?.email}")

        Log.d("RegisterActivity", "Navigation called from:")
        Thread.currentThread().stackTrace.take(10).forEach { element ->
            Log.d("RegisterActivity", "  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
        }


        try {
            val fragment = when (step) {
                1 -> {
                    Log.d("RegisterActivity", "Creating RegisterStep1Fragment")
                    RegisterStep1Fragment.newInstance()
                }
                2 -> {
                    Log.d("RegisterActivity", "Creating RegisterStep2Fragment")
                    RegisterStep2Fragment.newInstance(userData)
                }
                3 -> {
                    Log.d("RegisterActivity", "Creating RegisterStep3Fragment")
                    RegisterStep3Fragment.newInstance()
                }
                else -> {
                    Log.e("RegisterActivity", "Invalid step: $step")
                    return
                }
            }

            Log.d("RegisterActivity", "Fragment created, starting transaction")

            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)

            Log.d("RegisterActivity", "About to commit transaction")
            transaction.commit()

            Log.d("RegisterActivity", "Transaction committed")

            // Update ViewModel step
            registerViewModel.setStep(step)
            Log.d("RegisterActivity", "ViewModel step updated to: $step")

        } catch (e: Exception) {
            Log.e("RegisterActivity", "Exception in navigateToStep: ${e.message}", e)
            e.printStackTrace()
        }

        Log.d("RegisterActivity", "=== NAVIGATE TO STEP END ===")
    }

    // Handle Android back button - close activity or go to step 1
    override fun onBackPressed() {
        val currentStep = registerViewModel.currentStep.value ?: 1

        if (currentStep == 1) {
            // On step 1, exit the activity
            super.onBackPressed()
        } else {
            // On other steps, go back to step 1
            navigateToStep(1, null)
        }
    }
}