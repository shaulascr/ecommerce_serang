package com.alya.ecommerce_serang.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.chat.ChatItem
import com.alya.ecommerce_serang.data.api.response.chat.ChatItemList
import com.alya.ecommerce_serang.data.api.response.chat.ChatLine
import com.alya.ecommerce_serang.data.repository.ChatRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.utils.Constants
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socketService: SocketIOService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "ChatViewModel"

    // UI state using LiveData
    private val _state = MutableLiveData(ChatUiState())
    val state: LiveData<ChatUiState> = _state

    val _chatRoomId = MutableLiveData<Int>(0)
    val chatRoomId: LiveData<Int> = _chatRoomId

    private val _chatList = MutableLiveData<Result<List<ChatItemList>>>()
    val chatList: LiveData<Result<List<ChatItemList>>> = _chatList

    // Store and product parameters
    private var storeId: Int = 0
    private var productId: Int? = 0
    private var currentUserId: Int? = null
    private var defaultUserId: Int = 0

    // Product details for display
    private var productName: String = ""
    private var productPrice: String = ""
    private var productImage: String = ""
    private var productRating: Float = 0f
    private var storeName: String = ""

    // For image attachment
    private var selectedImageFile: File? = null

    init {
        // Try to get current user ID from the repository
        viewModelScope.launch {
            when (val result = chatRepository.fetchUserProfile()) {
                is Result.Success -> {
                    currentUserId = result.data?.userId
                    Log.e(TAG, "User ID: $currentUserId")

                    // Move the validation and subsequent logic inside the coroutine
                    if (currentUserId == null || currentUserId == 0) {
                        Log.e(TAG, "Error: User ID is not set or invalid")
                        updateState { it.copy(error = "User authentication error. Please login again.") }
                    } else {
                        // Set up socket listeners
                        setupSocketListeners()
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error fetching user profile: ${result.exception.message}")
                    updateState { it.copy(error = "User authentication error. Please login again.") }
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Set chat parameters received from activity
     */
    fun setChatParameters(
        storeId: Int,
        productId: Int? = 0,
        productName: String? = null,
        productPrice: String? = null,
        productImage: String? = null,
        productRating: Float? = 0f,
        storeName: String
    ) {
        this.storeId = storeId
        this.productId = productId!!
        this.productName = productName.toString()
        this.productPrice = productPrice.toString()
        this.productImage = productImage.toString()
        this.productRating = productRating!!
        this.storeName = storeName

        // Update state with product info
        updateState {
            it.copy(
                productName = productName.toString(),
                productPrice = productPrice.toString(),
                productImageUrl = productImage.toString(),
                productRating = productRating,
                storeName = storeName
            )
        }

        // Connect to socket and load chat history
        val existingChatRoomId = _chatRoomId.value ?: 0
        if (existingChatRoomId > 0) {
            // If we already have a chat room ID, we can load the chat history
            loadChatHistory(existingChatRoomId)

            // And join the Socket.IO room
            joinSocketRoom(existingChatRoomId)
        }
    }

    fun joinSocketRoom(roomId: Int) {
        if (roomId <= 0) {
            Log.e(TAG, "Cannot join room: Invalid room ID")
            return
        }

        socketService.joinRoom()
    }

    /**
     * Sets up listeners for Socket.IO events
     */
    private fun setupSocketListeners() {
        viewModelScope.launch {
            // Listen for connection state changes
            socketService.connectionState.collect { connectionState ->
                updateState { it.copy(connectionState = connectionState) }

                // Join room when connected
                if (connectionState is ConnectionState.Connected) {
                    socketService.joinRoom()
                }
            }
        }

        viewModelScope.launch {
            // Listen for new messages
            socketService.newMessages.collect { chatLine ->
                chatLine?.let {
                    val currentMessages = _state.value?.messages ?: listOf()
                    val updatedMessages = currentMessages.toMutableList().apply {
                        add(convertChatLineToUiMessage(it))
                    }
                    updateState { it.copy(messages = updatedMessages) }

                    // Update message status if received from others
                    if (it.senderId != currentUserId) {
                        updateMessageStatus(it.id, Constants.STATUS_READ)
                    }
                }
            }
        }

        viewModelScope.launch {
            // Listen for typing status updates
            socketService.typingStatus.collect { typingStatus ->
                typingStatus?.let {
                    if (typingStatus.roomId == (_chatRoomId.value ?: 0) && typingStatus.userId != currentUserId) {
                        updateState { it.copy(isOtherUserTyping = typingStatus.isTyping) }
                    }
                }
            }
        }
    }

    /**
     * Helper function to update LiveData state
     */
    private fun updateState(update: (ChatUiState) -> ChatUiState) {
        _state.value?.let {
            _state.value = update(it)
        }
    }

    /**
     * Loads chat history
     */
    fun loadChatHistory(chatRoomId: Int) {
        if (chatRoomId <= 0) {
            Log.e(TAG, "Cannot load chat history: Chat room ID is 0")
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            when (val result = chatRepository.getChatHistory(chatRoomId)) {
                is Result.Success -> {
                    val messages = result.data.chat.map { chatLine ->
                        convertChatLineToUiMessageHistory(chatLine)
                    }

                    updateState {
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            error = null
                        )
                    }

                    Log.d(TAG, "Loaded ${messages.size} messages for chat room $chatRoomId")

                    // Update status of unread messages
                    result.data.chat
                        .filter { it.senderId != currentUserId && it.status != Constants.STATUS_READ }
                        .forEach { updateMessageStatus(it.id, Constants.STATUS_READ) }
                }
                is Result.Error -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                    Log.e(TAG, "Error loading chat history: ${result.exception.message}")
                }
                is Result.Loading -> {
                    updateState { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Sends a chat message
     */
    fun sendMessage(message: String) {
        if (message.isBlank() && selectedImageFile == null) {
            Log.e(TAG, "Cannot send message: Both message and image are empty")
            return
        }

        // Check if we have the necessary parameters
        if (storeId <= 0) {
            Log.e(TAG, "Cannot send message: Store ID is invalid")
            updateState { it.copy(error = "Cannot send message. Invalid store ID.") }
            return
        }

        // Get the existing chatRoomId (not used in API but may be needed for Socket.IO)
        val existingChatRoomId = _chatRoomId.value ?: 0

        // Log debug information
        Log.d(TAG, "Sending message with params: storeId=$storeId, productId=$productId")
        Log.d(TAG, "Current user ID: $currentUserId")
        Log.d(TAG, "Has attachment: ${selectedImageFile != null}")

        // Check image file size if present
        selectedImageFile?.let { file ->
            if (file.exists() && file.length() > 5 * 1024 * 1024) { // 5MB limit
                updateState { it.copy(error = "Image file is too large. Please select a smaller image.") }
                return
            }
        }

        viewModelScope.launch {
            updateState { it.copy(isSending = true) }

            try {
                // Send the message using the repository
                // Note: We keep the chatRoomId parameter for compatibility with the repository method signature,
                // but it's not actually used in the API call
                val result = chatRepository.sendChatMessage(
                    storeId = storeId,
                    message = message,
                    productId = productId,
                    imageFile = selectedImageFile,
                    chatRoomId = existingChatRoomId
                )

                when (result) {
                    is Result.Success -> {
                        // Add new message to the list
                        val chatLine = result.data.chatLine
                        val newMessage = convertChatLineToUiMessage(chatLine)

                        val currentMessages = _state.value?.messages ?: listOf()
                        val updatedMessages = currentMessages.toMutableList().apply {
                            add(newMessage)
                        }

                        updateState {
                            it.copy(
                                messages = updatedMessages,
                                isSending = false,
                                hasAttachment = false,
                                error = null
                            )
                        }

                        Log.d(TAG, "Message sent successfully: ${chatLine.id}")

                        // Update the chat room ID if it's the first message
                        val newChatRoomId = chatLine.chatRoomId
                        if (existingChatRoomId == 0 && newChatRoomId > 0) {
                            Log.d(TAG, "Chat room created: $newChatRoomId")
                            _chatRoomId.value = newChatRoomId

                            // Now that we have a chat room ID, we can join the Socket.IO room
                            joinSocketRoom(newChatRoomId)
                        }

                        // Emit the message via Socket.IO for real-time updates
                        socketService.sendMessage(chatLine)

                        // Clear the image attachment
                        selectedImageFile = null
                    }
                    is Result.Error -> {
                        val errorMsg = if (result.exception.message.isNullOrEmpty() || result.exception.message == "{}") {
                            "Failed to send message. Please try again."
                        } else {
                            result.exception.message
                        }

                        updateState {
                            it.copy(
                                isSending = false,
                                error = errorMsg
                            )
                        }
                        Log.e(TAG, "Error sending message: ${result.exception.message}")
                    }
                    is Result.Loading -> {
                        updateState { it.copy(isSending = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendMessage", e)
                updateState {
                    it.copy(
                        isSending = false,
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates a message status (delivered, read)
     */
    fun updateMessageStatus(messageId: Int, status: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.updateMessageStatus(messageId, status)

                if (result is Result.Success) {
                    // Update local message status
                    val currentMessages = _state.value?.messages ?: listOf()
                    val updatedMessages = currentMessages.map { message ->
                        if (message.id == messageId) {
                            message.copy(status = status)
                        } else {
                            message
                        }
                    }
                    updateState { it.copy(messages = updatedMessages) }

                    Log.d(TAG, "Message status updated: $messageId -> $status")
                } else if (result is Result.Error) {
                    Log.e(TAG, "Error updating message status: ${result.exception.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating message status", e)
            }
        }
    }

    /**
     * Sets the selected image file for attachment
     */
    fun setSelectedImageFile(file: File?) {
        selectedImageFile = file
        updateState { it.copy(hasAttachment = file != null) }

        Log.d(TAG, "Image attachment ${if (file != null) "selected" else "cleared"}")
    }

    /**
     * Sends typing status to the other user
     */
    fun sendTypingStatus(isTyping: Boolean) {
        val roomId = _chatRoomId.value ?: 0
        if (roomId <= 0) return

        socketService.sendTypingStatus(roomId, isTyping)
    }

    /**
     * Clears any error message in the state
     */
    fun clearError() {
        updateState { it.copy(error = null) }
    }

    /**
     * Converts a ChatLine from API to a UI message model
     */
    private fun convertChatLineToUiMessage(chatLine: ChatLine): ChatUiMessage {
        // Format the timestamp for display
        val formattedTime = try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = inputFormat.parse(chatLine.createdAt)
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${chatLine.createdAt}", e)
            ""
        }

        return ChatUiMessage(
            id = chatLine.id,
            message = chatLine.message,
            attachment = chatLine.attachment ?: "", // Handle null attachment
            status = chatLine.status,
            time = formattedTime,
            isSentByMe = chatLine.senderId == currentUserId
        )
    }

    private fun convertChatLineToUiMessageHistory(chatItem: ChatItem): ChatUiMessage {
        // Format the timestamp for display
        val formattedTime = try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = inputFormat.parse(chatItem.createdAt)
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${chatItem.createdAt}", e)
            ""
        }

        return ChatUiMessage(
            attachment = chatItem.attachment, // Handle null attachment
            id = chatItem.id,
            message = chatItem.message,
            status = chatItem.status,
            time = formattedTime,
            isSentByMe = chatItem.senderId == currentUserId,
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Disconnect Socket.IO when ViewModel is cleared
        socketService.disconnect()
        Log.d(TAG, "ViewModel cleared, Socket.IO disconnected")
    }

    fun getChatList() {
        viewModelScope.launch {
            _chatList.value = com.alya.ecommerce_serang.data.repository.Result.Loading
            _chatList.value = chatRepository.getListChat()
        }
    }
}

/**
 * Data class representing the UI state for the chat screen
 */
data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val hasAttachment: Boolean = false,
    val isOtherUserTyping: Boolean = false,
    val error: String? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected(),

    // Product info
    val productName: String = "",
    val productPrice: String = "",
    val productImageUrl: String = "",
    val productRating: Float = 0f,
    val storeName: String = ""
)

/**
 * Data class representing a chat message in the UI
 */
data class ChatUiMessage(
    val id: Int,
    val message: String,
    val attachment: String?,
    val status: String,
    val time: String,
    val isSentByMe: Boolean
)