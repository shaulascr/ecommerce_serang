package com.alya.ecommerce_serang.ui.profile.editprofile

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityEditProfileCustBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditProfileCustActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileCustBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null

    private val viewModel: ProfileViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val userRepository = UserRepository(apiService)
            ProfileViewModel(userRepository)
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let {
                selectedImageUri = it

                val fullImageUrl = when (val img = selectedImageUri.toString()) {
                    is String -> {
                        if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                    }
                    else -> R.drawable.placeholder_image // Default image for null
                }

                Glide.with(this)
                    .load(fullImageUrl)
                    .into(binding.profileImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileCustBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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

        val userProfileJson = intent.getStringExtra("user_profile_json")
        val userProfile = if (userProfileJson != null) {
            val gson = Gson()
            gson.fromJson(userProfileJson, UserProfile::class.java)
        } else {
            null
        }

        userProfile?.let {
            populateFields(it)

            setupClickListeners()
            observeViewModel()
        }
    }
    private fun populateFields(profile: UserProfile) {
        binding.etNameUser.setText(profile.name)
        binding.etUsername.setText(profile.username)
        binding.etEmailUser.setText(profile.email)
        binding.etNumberPhoneUser.setText(profile.phone)

        // Format birth date for display
        profile.birthDate?.let {
            binding.etDateBirth.setText(formatDate(it))
        }

        val fullImageUrl = when (val img = profile.image) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image // Default image for null
        }

        // Load profile image
        if (fullImageUrl != null && fullImageUrl is String) {
            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.baseline_account_circle_24)
                .into(binding.profileImage)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.editIcon.setOnClickListener {
            openImagePicker()
        }

        binding.tvSelectImage.setOnClickListener {
            openImagePicker()
        }

        binding.etDateBirth.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun openImagePicker() {
        // Check for permission first
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            launchImagePicker()
        }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // If there's already a date in the field, parse it
        val dateText = binding.etDateBirth.text.toString()
        if (dateText.isNotEmpty() && dateText != "N/A" && dateText != "Invalid Date") {
            try {
                val displayFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                val date = displayFormat.parse(dateText)
                date?.let {
                    calendar.time = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${e.message}")
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val displayFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                val formattedDate = displayFormat.format(calendar.time)
                binding.etDateBirth.setText(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveProfile() {
        val name = binding.etNameUser.text.toString()
        val username = binding.etUsername.text.toString()
        val email = binding.etEmailUser.text.toString()
        val phone = binding.etNumberPhoneUser.text.toString()
        val displayDate = binding.etDateBirth.text.toString()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty() || displayDate.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert date to server format
        val serverBirthDate = convertToServerDateFormat(displayDate)

        Log.d(TAG, "Starting profile save with direct method")
        Log.d(TAG, "Selected image URI: $selectedImageUri")

        // Disable the button to prevent multiple clicks
        binding.btnSave.isEnabled = false

        // Call the repository method via ViewModel
        viewModel.editProfileDirect(
            context = this,  // Pass context for file operations
            username = username,
            name = name,
            phone = phone,
            birthDate = serverBirthDate,
            email = email,
            imageUri = selectedImageUri
        )
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        Log.d(TAG, "Getting real path from URI: $uri")

        // Handle different URI schemes
        when {
            // File URI
            uri.scheme == "file" -> {
                val path = uri.path
                Log.d(TAG, "URI is file scheme, path: $path")
                return path
            }

            // Content URI
            uri.scheme == "content" -> {
                try {
                    val projection = arrayOf(MediaStore.Images.Media.DATA)
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                            val path = cursor.getString(columnIndex)
                            Log.d(TAG, "Found path from content URI: $path")
                            return path
                        } else {
                            Log.e(TAG, "Cursor is empty")
                        }
                    } ?: Log.e(TAG, "Cursor is null")

                    // If the above fails, try the documented API way
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        // Create a temp file
                        val fileName = getFileName(uri) ?: "temp_img_${System.currentTimeMillis()}.jpg"
                        val tempFile = File(cacheDir, fileName)
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        Log.d(TAG, "Created temporary file: ${tempFile.absolutePath}")
                        return tempFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting real path: ${e.message}", e)
                }
            }
        }

        Log.e(TAG, "Could not get real path for URI: $uri")
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                        Log.d(TAG, "Found filename from content URI: $result")
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
            Log.d(TAG, "Extracted filename from path: $result")
        }
        return result
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return "Invalid Date")
        } catch (e: Exception) {
            Log.e("ERROR", "Date parsing error: ${e.message}")
            "Invalid Date"
        }
    }

    private fun convertToServerDateFormat(displayDate: String): String {
        return try {
            val displayFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
            val date = displayFormat.parse(displayDate) ?: return ""

            val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            serverFormat.format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting date format: ${e.message}")
            ""
        }
    }

    private fun observeViewModel() {
        viewModel.editProfileResult.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    // Show loading indicator
                    binding.btnSave.isEnabled = false
                }
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    // Show success message
                    Toast.makeText(this, result.data.message, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    // Show error message
                    Toast.makeText(this, result.exception.message ?: "Error updating profile", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchImagePicker()
        } else {
            Toast.makeText(this, "Permission needed to select image", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
        private const val TAG = "EditProfileCustActivity"
    }
}