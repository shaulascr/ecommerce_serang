package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.UpdateChatRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.chat.ChatHistoryResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatItemList
import com.alya.ecommerce_serang.data.api.response.chat.SendChatResponse
import com.alya.ecommerce_serang.data.api.response.chat.UpdateChatResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
        productId: Int? = null,
        imageFile: File? = null,
        chatRoomId: Int? = null // Not used in the actual API call but kept for compatibility
    ): Result<SendChatResponse> {
        return try {
            // Create multipart request parts
            val storeIdPart = storeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val messagePart = message.toRequestBody("text/plain".toMediaTypeOrNull())

            // Add product ID part if provided
            val productIdPart = if (productId != null && productId > 0) {
                productId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }

            // Create image part if file is provided
            val imagePart = if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("chatimg", imageFile.name, requestFile)
            } else {
                null
            }

            // Debug log the request parameters
            Log.d("ChatRepository", "Sending chat with: storeId=$storeId, productId=$productId, " +
                    "message length=${message.length}, hasImage=${imageFile != null}")

            // Make API call using your actual endpoint and parameter names
            val response = apiService.sendChatLine(
                storeId = storeIdPart,
                message = messagePart,
                productId = productIdPart,
                chatimg = imagePart
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    Result.Error(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "{}"
                Log.e("ChatRepository", "API Error: ${response.code()} - $errorBody")
                Result.Error(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception sending message", e)
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

    suspend fun getListChat(): Result<List<ChatItemList>> {
        return try {
            val response = apiService.getChatList()

            if (response.isSuccessful){
                val chat = response.body()?.chat ?: emptyList()
                Result.Success(chat)
            } else {
                Result.Error(Exception("Failed to fetch categories. Code: ${response.code()}"))
            }
        } catch (e: Exception){
            Result.Error(e)
        }
    }
}