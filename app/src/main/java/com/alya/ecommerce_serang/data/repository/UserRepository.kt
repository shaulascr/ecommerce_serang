package com.alya.ecommerce_serang.data.repository

import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.response.OtpResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService

class UserRepository(private val apiService: ApiService) {

    suspend fun requestOtpRep(email: String): OtpResponse {

//    fun requestOtpRep(email: String): Result<String> {

        return apiService.getOTP(OtpRequest(email))

    }

    suspend fun registerUser(request: RegisterRequest): String {
        val response = apiService.register(request) // API call

        if (response.isSuccessful) {
            val responseBody = response.body() ?: throw Exception("Empty response body")
            return responseBody.message // Get the message from RegisterResponse
        } else {
            throw Exception("Registration failed: ${response.errorBody()?.string()}")
        }
    }
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}