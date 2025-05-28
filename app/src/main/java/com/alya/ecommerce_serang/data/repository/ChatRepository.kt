package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.UpdateChatRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.chat.ChatHistoryResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatItemList
import com.alya.ecommerce_serang.data.api.response.chat.SendChatResponse
import com.alya.ecommerce_serang.data.api.response.chat.UpdateChatResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
        productId: Int?,           // Nullable and optional
        imageFile: File? = null    // Nullable and optional
    ): Result<SendChatResponse> {
        return try {
            val parts = mutableMapOf<String, RequestBody>()

            // Required fields
            parts["store_id"] = storeId.toString().toRequestBody("text/plain".toMediaType())
            parts["message"] = message.toRequestBody("text/plain".toMediaType())

            // Optional: Only include if productId is valid
            if (productId != null && productId > 0) {
                parts["product_id"] = productId.toString().toRequestBody("text/plain".toMediaType())
            }

            // Optional: Only include if imageFile is valid
            val imagePart = imageFile?.takeIf { it.exists() }?.let { file ->
//                val requestFile = file.asRequestBody("image/*".toMediaType())
                val mimeType = when {
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    else -> "image/jpeg"  // fallback
                }
                val requestFile = file.asRequestBody(mimeType.toMediaType())
                MultipartBody.Part.createFormData("chatimg", file.name, requestFile)
            }

            // Log the parts map keys and values (string representations)
            Log.d("ChatRepository", "Sending chat message with parts:")
            parts.forEach { (key, body) ->
                Log.d("ChatRepository", "Key: $key, Value (approx): ${bodyToString(body)}")
            }
            Log.d("ChatRepository", "Sending chat message with imagePart: ${imagePart != null}")

            // Send request
            val response = apiService.sendChatMessage(parts, imagePart)

            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) } ?: Result.Error(Exception("Empty response body"))
            } else {
                val errorMsg = response.errorBody()?.string().orEmpty()
                Log.e("ChatRepository", "API Error: ${response.code()} - $errorMsg")
                Result.Error(Exception("API Error: ${response.code()} - $errorMsg"))
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception sending chat message", e)
            Result.Error(e)
        }
    }

    suspend fun sendChatMessageStore(
        storeId: Int,
        message: String,
        productId: Int?,           // Nullable and optional
        imageFile: File? = null    // Nullable and optional
    ): Result<SendChatResponse> {
        return try {
            val parts = mutableMapOf<String, RequestBody>()

            // Required fields
            parts["store_id"] = storeId.toString().toRequestBody("text/plain".toMediaType())
            parts["message"] = message.toRequestBody("text/plain".toMediaType())

            // Optional: Only include if productId is valid
            if (productId != null && productId > 0) {
                parts["product_id"] = productId.toString().toRequestBody("text/plain".toMediaType())
            }

            // Optional: Only include if imageFile is valid
            val imagePart = imageFile?.takeIf { it.exists() }?.let { file ->
//                val requestFile = file.asRequestBody("image/*".toMediaType())
                val mimeType = when {
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    else -> "image/jpeg"  // fallback
                }
                val requestFile = file.asRequestBody(mimeType.toMediaType())
                MultipartBody.Part.createFormData("chatimg", file.name, requestFile)
            }

            // Log the parts map keys and values (string representations)
            Log.d("ChatRepository", "Sending chat message with parts:")
            parts.forEach { (key, body) ->
                Log.d("ChatRepository", "Key: $key, Value (approx): ${bodyToString(body)}")
            }
            Log.d("ChatRepository", "Sending chat message with imagePart: ${imagePart != null}")

            // Send request
            val response = apiService.sendChatMessageStore(parts, imagePart)

            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) } ?: Result.Error(Exception("Empty response body"))
            } else {
                val errorMsg = response.errorBody()?.string().orEmpty()
                Log.e("ChatRepository", "API Error: ${response.code()} - $errorMsg")
                Result.Error(Exception("API Error: ${response.code()} - $errorMsg"))
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception sending chat message", e)
            Result.Error(e)
        }
    }

    // Helper function to get string content from RequestBody (best effort)
    private fun bodyToString(requestBody: RequestBody): String {
        return try {
            val buffer = okio.Buffer()
            requestBody.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            "Could not read body"
        }
    }

//    suspend fun sendChatMessage(
//        storeId: Int,
//        message: String,
//        productId: Int?,
//        imageFile: File? = null,
//        chatRoomId: Int? = null
//    ): Result<SendChatResponse> {
//        return try {
//            Log.d(TAG, "=== SEND CHAT MESSAGE ===")
//            Log.d(TAG, "StoreId: $storeId")
//            Log.d(TAG, "Message: '$message'")
//            Log.d(TAG, "ProductId: $productId")
//            Log.d(TAG, "ImageFile: ${imageFile?.absolutePath}")
//            Log.d(TAG, "ImageFile exists: ${imageFile?.exists()}")
//            Log.d(TAG, "ImageFile size: ${imageFile?.length()} bytes")
//
//            // Convert primitive fields to RequestBody
//            val storeIdBody = storeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
//            val messageBody = message.toRequestBody("text/plain".toMediaTypeOrNull())
//            val productIdBody = productId?.takeIf { it > 0 } // null if 0
//                ?.toString()
//                ?.toRequestBody("text/plain".toMediaTypeOrNull())
//
//            // Convert image file to MultipartBody.Part if exists
//            val imagePart: MultipartBody.Part? = imageFile?.takeIf { it.exists() }?.let { file ->
//                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
//                MultipartBody.Part.createFormData("chatimg", file.name, requestFile)
//            }
//
//
//
//            // Call the API
//            Log.d(TAG, "Sending request. ProductIdBody is null: ${productIdBody == null}")
//
//            val response = apiService.sendChatLine(
//                storeId = storeIdBody,
//                message = messageBody,
//                productId = productIdBody,
//                chatimg = imagePart
//            )
//
//            // Handle API response
//            if (response.isSuccessful) {
//                response.body()?.let {
//                    Log.d(TAG, "Success: ${it.message}")
//                    Result.Success(it)
//                } ?: run {
//                    Log.e(TAG, "Response body is null")
//                    Result.Error(Exception("Empty response body"))
//                }
//            } else {
//                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
//                Log.e(TAG, "API Error: ${response.code()} - $errorMsg")
//                Result.Error(Exception("API Error: ${response.code()} - $errorMsg"))
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Exception sending chat message", e)
//            Result.Error(e)
//        }
//    }

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

    suspend fun getListChatStore(): Result<List<ChatItemList>> {
        return try {
            Log.d("ChatRepository", "Calling getChatListStore() from ApiService")

            val response = apiService.getChatListStore()

            Log.d("ChatRepository", "Response received: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (response.isSuccessful) {
                val chat = response.body()?.chat ?: emptyList()
                Log.d("ChatRepository", "Chat list size: ${chat.size}")
                Result.Success(chat)
            } else {
                Log.e("ChatRepository", "Failed response: ${response.errorBody()?.string()}")
                Result.Error(Exception("Failed to fetch chat list. Code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception during getChatListStore", e)
            Result.Error(e)
        }
    }
}