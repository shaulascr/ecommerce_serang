package com.alya.ecommerce_serang.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.EditProfileResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.utils.FileUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class UserRepository(private val apiService: ApiService) {
    //post data without message/response
    suspend fun requestOtpRep(email: String): OtpResponse {
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

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Login response is empty"))
            } else {
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchUserProfile(): Result<UserProfile?> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                response.body()?.user?.let {
                    Result.Success(it)  // ✅ Returning only UserProfile
                } ?: Result.Error(Exception("User data not found"))
            } else {
                Result.Error(Exception("Error fetching profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun editProfileCust(
        context: Context,
        username: String,
        name: String,
        phone: String,
        birthDate: String,
        email: String,
        imageUri: Uri?
    ): Result<EditProfileResponse> {
        return try {
            // Log the data being sent
            Log.d(TAG, "Edit Profile - Username: $username, Name: $name, Phone: $phone, Birth Date: $birthDate, Email: $email")
            Log.d(TAG, "Image URI: $imageUri")

            // Create RequestBody objects for text fields
            val usernameRequestBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneRequestBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val birthDateRequestBody = birthDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailRequestBody = email.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create MultipartBody.Part for the image
            val imagePart = if (imageUri != null) {
                // Create a temporary file from the URI using the utility class
                val imageFile = FileUtils.createTempFileFromUri(context, imageUri, "profile")
                if (imageFile != null) {
                    // Create MultipartBody.Part from the file
                    FileUtils.createMultipartFromFile("userimg", imageFile)
                } else {
                    // Fallback to empty part
                    FileUtils.createEmptyMultipart("userimg")
                }
            } else {
                // No image selected, use empty part
                FileUtils.createEmptyMultipart("userimg")
            }

            // Make the API call
            val response = apiService.editProfileCustomer(
                username = usernameRequestBody,
                name = nameRequestBody,
                phone = phoneRequestBody,
                birthDate = birthDateRequestBody,
                userimg = imagePart,
                email = emailRequestBody
            )

            // Process the response
            if (response.isSuccessful) {
                val editResponse = response.body()
                if (editResponse != null) {
                    Log.d(TAG, "Edit profile success: ${editResponse.message}")
                    Result.Success(editResponse)
                } else {
                    Log.e(TAG, "Response body is null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown Error"
                Log.e(TAG, "Error editing profile: $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in editProfileCust: ${e.message}")
            e.printStackTrace()
            Result.Error(e)
        }
    }

    companion object{
        private const val TAG = "UserRepository"
    }


}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}