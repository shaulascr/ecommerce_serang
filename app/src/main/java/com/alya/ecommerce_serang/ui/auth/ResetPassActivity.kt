package com.alya.ecommerce_serang.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityResetPassBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.viewmodel.LoginViewModel

class ResetPassActivity : AppCompatActivity() {

    private val TAG = "ResetPassActivity"
    private lateinit var binding: ActivityResetPassBinding

    private val loginViewModel: LoginViewModel by viewModels{
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val userRepository = UserRepository(apiService)
            LoginViewModel(userRepository, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        enableEdgeToEdge()

        setupToolbar()
        setupUI()

    }

    private fun setupToolbar(){
        binding.headerResetPass.headerLeftIcon.setOnClickListener{
            finish()
        }
        binding.headerResetPass.headerTitle.text = "Lupa Password"
    }

    private fun setupUI(){
        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                loginViewModel.resetPassword(email)
            } else {
                binding.etEmail.error = "Masukkan Email Anda"
            }
        }
    }

    private fun observeResetPassword() {
        loginViewModel.resetPasswordState.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    showLoading(true)
                }

                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    showLoading(false)
                    handleSuccess("Silahkan cek email anda untuk melihat password anda.")
                    Log.d(TAG, "Success rest password: ${result.data.message}")
                }

                is Result.Error -> {
                    showLoading(false)
                    handleError("Email anda salah atau tidak ditemukan.")
                    Log.e(TAG, "Error reset password ${result.exception.message}")
                }

                null -> {
                    // Initial state
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnReset.isEnabled = false
            binding.etEmail.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnReset.isEnabled = true
            binding.etEmail.isEnabled = true
        }
    }

    private fun handleSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Show success dialog and navigate back to login
        AlertDialog.Builder(this)
            .setTitle("Berhasil Ubah Password")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Navigate back to login activity
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun handleError(errorMessage: String) {
        Log.e(TAG, "Error: $errorMessage")

        // Optionally show error dialog
        AlertDialog.Builder(this)
            .setTitle("Gagal Ubah Password")
            .setMessage(errorMessage)
            .setPositiveButton("OK", null)
            .show()
    }
}