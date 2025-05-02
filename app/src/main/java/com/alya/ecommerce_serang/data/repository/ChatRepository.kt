package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.UpdateChatRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.chat.ChatHistoryResponse
import com.alya.ecommerce_serang.data.api.response.chat.SendChatResponse
import com.alya.ecommerce_serang.data.api.response.chat.UpdateChatResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val TAG = "ChatRepository"

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

    suspend fun sendChatMessage(
        storeId: Int,
        message: String,
        productId: Int,
        imageFile: File? = null
    ): Result<SendChatResponse> {
        return try {
            // Create request bodies for text fields
            val storeIdBody = RequestBody.create("text/plain".toMediaTypeOrNull(), storeId.toString())
            val messageBody = RequestBody.create("text/plain".toMediaTypeOrNull(), message)
            val productIdBody = RequestBody.create("text/plain".toMediaTypeOrNull(), productId.toString())

            // Create multipart body for the image file
            val imageMultipart = if (imageFile != null && imageFile.exists()) {
                // Log detailed file information
                Log.d(TAG, "Image file: ${imageFile.absolutePath}")
                Log.d(TAG, "Image file size: ${imageFile.length()} bytes")
                Log.d(TAG, "Image file exists: ${imageFile.exists()}")
                Log.d(TAG, "Image file can read: ${imageFile.canRead()}")

                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)
                MultipartBody.Part.createFormData("chatimg", imageFile.name, requestFile)
            } else {
                // Pass null when no image is provided
                null
            }

            // Log request info
            Log.d(TAG, "Sending message to store ID: $storeId, product ID: $productId")
            Log.d(TAG, "Message content: $message")
            Log.d(TAG, "Has image: ${imageFile != null && imageFile.exists()}")

            // Make the API call
            val response = apiService.sendChatLine(
                storeId = storeIdBody,
                message = messageBody,
                productId = productIdBody,
                chatimg = imageMultipart
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Send chat response is empty"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "HTTP Error: ${response.code()}, Body: $errorBody")
                Result.Error(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending message", e)
            e.printStackTrace()
            Result.Error(e)
        }
    }

    suspend fun updateMessageStatus(
        messageId: Int,
        status: String
    ): Result<UpdateChatResponse> {
        return try {
            val requestBody = UpdateChatRequest(
                id = messageId,
                status = status
            )

            val response = apiService.updateChatStatus(requestBody)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Update status response is empty"))
            } else {
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getChatHistory(chatRoomId: Int): Result<ChatHistoryResponse> {
        return try {
            val response = apiService.getChatDetail(chatRoomId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Chat history response is empty"))
            } else {
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}