package com.alya.ecommerce_serang.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailProfileBinding
import com.alya.ecommerce_serang.ui.profile.editprofile.EditProfileCustActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetailProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProfileBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var currentUserProfile: UserProfile? = null


    private val viewModel: ProfileViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val userRepository = UserRepository(apiService)
            ProfileViewModel(userRepository)
        }
    }

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh profile after edit
            viewModel.loadUserProfile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        setupClickListeners()

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

        viewModel.loadUserProfile()

        viewModel.userProfile.observe(this) { user ->
            Log.d("DetailProfileActivity", "Observed userProfile: $user")
            user?.let {
                updateProfile(it)
            } ?: run {
                Log.e("DetailProfileActivity", "Received null user profile from ViewModel")
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            Log.e("DetailProfileActivity", "Error from ViewModel: $error")
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }


    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnUbahProfil.setOnClickListener {
            currentUserProfile?.let { profile ->
                val gson = Gson()
                val userProfileJson = gson.toJson(currentUserProfile)
                val intent = Intent(this, EditProfileCustActivity::class.java).apply {
                    putExtra("user_profile_json", userProfileJson)
                }
                editProfileLauncher.launch(intent)
            } ?: run {
                Toast.makeText(this, "Akun tidak ditemukan", Toast.LENGTH_SHORT).show()
                Log.e("DetailProfileActivity", "Profile data is not available")
            }
        }
    }

    private fun updateProfile(user: UserProfile) {
        Log.d("DetailProfileActivity", "updateProfile called with user: $user")

        // Store the user profile for later use
        currentUserProfile = user

        binding.tvNameUser.setText(user.name.toString())
        binding.tvUsername.setText(user.username)
        binding.tvEmailUser.setText(user.email)
        Log.d("ProfileActivity", "Raw Birth Date: ${user.birthDate}")
        binding.tvDateBirth.setText(user.birthDate?.let { formatDate(it) } ?: "N/A")
        Log.d("ProfileActivity", "Formatted Birth Date: ${formatDate(user.birthDate)}")
        binding.tvNumberPhoneUser.setText(user.phone)

        val fullImageUrl = when (val img = user.image) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image // Default image for null
        }

        if (fullImageUrl != null && fullImageUrl is String) {
            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.baseline_account_circle_24)
                .into(binding.profileImage)
        }
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A" // Return default if null

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Ensure parsing in UTC

            val outputFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault()) // Convert to "dd-MM-yy" format
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return "Invalid Date") // Ensure valid date
        } catch (e: Exception) {
            Log.e("ERROR", "Date parsing error: ${e.message}") // Log errors for debugging
            "Invalid Date"
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this screen
        viewModel.loadUserProfile()
    }

}