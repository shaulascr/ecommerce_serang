package com.alya.ecommerce_serang.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityChangePasswordBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ProfileViewModel
import kotlin.getValue

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private val viewModel: ProfileViewModel by viewModels {
        BaseViewModelFactory {
            apiService = ApiConfig.getApiService(sessionManager)
            val userRepository = UserRepository(apiService)
            ProfileViewModel(userRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.header.headerTitle.text = "Ubah Kata Sandi"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        // Listen for the result of the password change
        viewModel.changePasswordResult.observe(this, Observer { result ->
            when (result) {
                is Result.Error -> {
                    Toast.makeText(
                        this,
                        "Gagal mengubah kata sandi: ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Result.Success -> {
                    Toast.makeText(this, "Berhasil mengubah kata sandi", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Loading -> {}
            }
        })

        // Button to trigger password change
        binding.btnChangePass.setOnClickListener {
            val currentPassword = binding.etLoginPassword.text.toString()
            val newPassword = binding.etLoginNewPassword.text.toString()

            if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                // Call change password function from ViewModel
                viewModel.changePassword(currentPassword, newPassword)
            } else {
                Toast.makeText(this, "Lengkapi data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}