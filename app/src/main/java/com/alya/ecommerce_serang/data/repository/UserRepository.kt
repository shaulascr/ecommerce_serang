package com.alya.ecommerce_serang.data.repository

import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService

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

//    suspend fun sendChatMessage(
//        storeId: Int,
//        message: String,
//        productId: Int,
//        imageFile: File? = null
//    ): Result<SendChatResponse> {
//        return try {
//            // Create multipart request builder
//            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
//
//            // Add text fields
//            requestBodyBuilder.addFormDataPart("store_id", storeId.toString())
//            requestBodyBuilder.addFormDataPart("message", message)
//            requestBodyBuilder.addFormDataPart("product_id", productId.toString())
//
//            // Add image if it exists
//            if (imageFile != null && imageFile.exists()) {
//                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//                requestBodyBuilder.addFormDataPart("chatimg", imageFile.name, requestFile)
//            }
//
//            // Build the final request body
//            val requestBody = requestBodyBuilder.build()
//
//            // Make the API call using a custom endpoint that takes a plain MultipartBody
//            val response = apiService.sendChatLineWithBody(requestBody)
//
//            if (response.isSuccessful) {
//                response.body()?.let {
//                    Result.Success(it)
//                } ?: Result.Error(Exception("Send chat response is empty"))
//            } else {
//                val errorBody = response.errorBody()?.string() ?: "Unknown error"
//                Log.e("ChatRepository", "HTTP Error: ${response.code()}, Body: $errorBody")
//                Result.Error(Exception("API Error: ${response.code()} - $errorBody"))
//            }
//        } catch (e: Exception) {
//            Log.e("ChatRepository", "Exception sending message", e)
//            e.printStackTrace()
//            Result.Error(e)
//        }
//    }
//
//    /**
//     * Updates the status of a message (sent, delivered, read)
//     *
//     * @param messageId The ID of the message to update
//     * @param status The new status to set
//     * @return Result containing the updated message details or error
//     */
//    suspend fun updateMessageStatus(
//        messageId: Int,
//        status: String
//    ): Result<UpdateChatResponse> {
//        return try {
//            val requestBody = UpdateChatRequest(
//                id = messageId,
//                status = status
//            )
//
//            val response = apiService.updateChatStatus(requestBody)
//
//            if (response.isSuccessful) {
//                response.body()?.let {
//                    Result.Success(it)
//                } ?: Result.Error(Exception("Update status response is empty"))
//            } else {
//                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
//            }
//        } catch (e: Exception) {
//            Result.Error(e)
//        }
//    }
//
//    /**
//     * Gets the chat history for a specific chat room
//     *
//     * @param chatRoomId The ID of the chat room
//     * @return Result containing the list of chat messages or error
//     */
//    suspend fun getChatHistory(chatRoomId: Int): Result<ChatHistoryResponse> {
//        return try {
//            val response = apiService.getChatDetail(chatRoomId)
//
//            if (response.isSuccessful) {
//                response.body()?.let {
//                    Result.Success(it)
//                } ?: Result.Error(Exception("Chat history response is empty"))
//            } else {
//                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
//            }
//        } catch (e: Exception) {
//            Result.Error(e)
//        }
//    }


}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}