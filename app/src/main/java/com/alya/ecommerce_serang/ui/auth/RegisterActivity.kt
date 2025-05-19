package com.alya.ecommerce_serang.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityRegisterBinding
import com.alya.ecommerce_serang.ui.MainActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.RegisterViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    private var isEmailValid = false
    private var isPhoneValid = false

    // Track which validation was last performed
    private var lastCheckField = ""

    // Counter for signup validation
    private var signupValidationsComplete = 0
    private var signupInProgress = false

    private val registerViewModel: RegisterViewModel by viewModels{
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val userRepository = UserRepository(apiService)
            RegisterViewModel(userRepository)
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

        setupObservers()

        // Set up field validations
        setupFieldValidations()

        binding.btnSignup.setOnClickListener {
            handleSignUp()
        }

        binding.tvLoginAlt.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.etBirthDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupFieldValidations() {
        // Validate email when focus changes
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.etEmail.text.toString()
                if (email.isNotEmpty()) {
                    validateEmail(email, false)
                }
            }
        }

        // Validate phone when focus changes
        binding.etNumberPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val phone = binding.etNumberPhone.text.toString()
                if (phone.isNotEmpty()) {
                    validatePhone(phone, false)
                }
            }
        }
    }

    private fun validateEmail(email: String, isSignup: Boolean) {
        lastCheckField = "email"
        Log.d("RegisterActivity", "Validating email: $email (signup: $isSignup)")

        val checkValueEmail = VerifRegisReq(
            fieldRegis = "email",
            valueRegis = email
        )
        registerViewModel.checkValueReg(checkValueEmail)
    }

    private fun validatePhone(phone: String, isSignup: Boolean) {
        lastCheckField = "phone"
        Log.d("RegisterActivity", "Validating phone: $phone (signup: $isSignup)")

        val checkValuePhone = VerifRegisReq(
            fieldRegis = "phone",
            valueRegis = phone
        )
        registerViewModel.checkValueReg(checkValuePhone)
    }

    private fun setupObservers() {

        registerViewModel.checkValue.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading if needed
                }
                is Result.Success -> {
                    val isValid = (result.data as? Boolean) ?: false

                    when (lastCheckField) {
                        "email" -> {
                            isEmailValid = isValid
                            if (!isValid) {
                                Toast.makeText(this, "Email is already registered", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d("RegisterActivity", "Email is valid")
                            }
                        }
                        "phone" -> {
                            isPhoneValid = isValid
                            if (!isValid) {
                                Toast.makeText(this, "Phone number is already registered", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d("RegisterActivity", "Phone is valid")
                            }
                        }
                    }

                    // Check if we're in signup process
                    if (signupInProgress) {
                        signupValidationsComplete++

                        // Check if both validations completed
                        if (signupValidationsComplete >= 2) {
                            signupInProgress = false
                            signupValidationsComplete = 0

                            // If both validations passed, request OTP
                            if (isEmailValid && isPhoneValid) {
                                requestOtp()
                            }
                        }
                    }
                }
                is Result.Error -> {
                    val fieldType = if (lastCheckField == "email") "Email" else "Phone"
                    Toast.makeText(this, "$fieldType validation failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()

                    // Mark validation as invalid
                    if (lastCheckField == "email") {
                        isEmailValid = false
                    } else if (lastCheckField == "phone") {
                        isPhoneValid = false
                    }

                    // Update signup validation counter if in signup process
                    if (signupInProgress) {
                        signupValidationsComplete++

                        // Check if both validations completed
                        if (signupValidationsComplete >= 2) {
                            signupInProgress = false
                            signupValidationsComplete = 0
                        }
                    }
                }
                else -> {
                    Log.e("RegisterActivity", "Unexpected result type: $result")
                }
            }
        }
        registerViewModel.otpState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBarOtp.visibility = android.view.View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBarOtp.visibility = android.view.View.GONE
                    Log.d("RegisterActivity", "OTP sent successfully. Showing OTP dialog.")

                    // Create user data before showing OTP dialog
                    val userData = createUserData()

                    // Show OTP dialog
                    val otpBottomSheet = OtpBottomSheetDialog(userData) { fullUserData ->
                        Log.d("RegisterActivity", "OTP entered successfully. Proceeding with registration.")
                        registerViewModel.registerUser(fullUserData)
                    }
                    otpBottomSheet.show(supportFragmentManager, "OtpBottomSheet")
                }
                is Result.Error -> {
                    binding.progressBarOtp.visibility = android.view.View.GONE
                    Toast.makeText(this, "OTP Request Failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.e("RegisterActivity", "Unexpected result type: $result")
                }
            }
        }
        registerViewModel.registerState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading indicator for registration
                    binding.progressBarRegister.visibility = android.view.View.VISIBLE
                }
                is Result.Success -> {
                    // Hide loading indicator and show success message
                    binding.progressBarRegister.visibility = android.view.View.GONE
                    Toast.makeText(this, result.data, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    // Navigate to another screen if needed
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    // Hide loading indicator and show error message
                    binding.progressBarRegister.visibility = android.view.View.GONE
                    Toast.makeText(this, "Registration Failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSignUp() {
        // Basic validation first
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val phone = binding.etNumberPhone.text.toString()
        val username = binding.etUsername.text.toString()
        val name = binding.etFullname.text.toString()
        val birthDate = binding.etBirthDate.text.toString()

        // Check if fields are filled
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            phone.isEmpty() || username.isEmpty() || name.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // If both validations are already done and successful, just request OTP
        if (isEmailValid && isPhoneValid) {
            requestOtp()
            return
        }

        // Reset validation counters
        signupInProgress = true
        signupValidationsComplete = 0

        // Start validations in parallel
        validateEmail(email, true)
        validatePhone(phone, true)
    }

    private fun requestOtp() {
        val email = binding.etEmail.text.toString()
        Log.d("RegisterActivity", "Requesting OTP for email: $email")
        registerViewModel.requestOtp(email)
    }

    private fun createUserData(): RegisterRequest {
        // Get all form values
        val birthDate = binding.etBirthDate.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val phone = binding.etNumberPhone.text.toString()
        val username = binding.etUsername.text.toString()
        val name = binding.etFullname.text.toString()
        val image = null

        // Create and return user data object
        return RegisterRequest(name, email, password, username, phone, birthDate, image)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etBirthDate.setText(sdf.format(calendar.time))
            },
            year, month, day
        ).show()
    }
}