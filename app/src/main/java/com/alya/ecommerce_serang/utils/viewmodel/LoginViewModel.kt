package com.alya.ecommerce_serang.utils.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.FcmReq
import com.alya.ecommerce_serang.data.api.dto.ResetPassReq
import com.alya.ecommerce_serang.data.api.response.auth.FcmTokenResponse
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.ResetPassResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.utils.SessionManager
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserRepository, private val context: Context) : ViewModel() {
    private val _loginState = MutableLiveData<Result<LoginResponse>>()
    val loginState: LiveData<Result<LoginResponse>> get() = _loginState

    private val _otpState = MutableLiveData<Result<Unit>>()
    val otpState: LiveData<Result<Unit>> = _otpState

    // MutableLiveData to store messages from API responses
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _resetPasswordState = MutableLiveData<Result<ResetPassResponse>?>()
    val resetPasswordState: LiveData<Result<ResetPassResponse>?> = _resetPasswordState


    private val sessionManager by lazy { SessionManager(context) }

    private fun getAuthenticatedApiService(): ApiService {
        return ApiConfig.getApiService(sessionManager)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Result.Loading
            val result = repository.login(email, password)
            _loginState.value = result
        }
    }

    fun sendFcm(token: FcmReq) {
        viewModelScope.launch {
            _otpState.value = Result.Loading // Indicating API call in progress

            try {
                // Call the repository function to request OTP
                val authenticatedApiService = getAuthenticatedApiService()
                val authenticatedOrderRepo = UserRepository(authenticatedApiService)
                val response: FcmTokenResponse = authenticatedOrderRepo.sendFcm(token)

                // Log and store success message
                Log.d("LoginViewModel", "OTP Response: ${response.message}")
                _message.value = response.message ?: "berhasil" // Store the message for UI feedback

                // Update state to indicate success
                _otpState.value = Result.Success(Unit)

            } catch (exception: Exception) {
                // Handle any errors and update state
                _otpState.value = Result.Error(exception)
                _message.value = exception.localizedMessage ?: "Failed to request OTP"

                // Log the error for debugging
                Log.e("LoginViewModel", "OTP request failed for: $token", exception)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = Result.Loading

            val request = ResetPassReq(emailOrPhone = email)
            val result = repository.resetPassword(request)

            _resetPasswordState.value = result
        }
    }

    fun clearState() {
        _resetPasswordState.value = null
    }

}