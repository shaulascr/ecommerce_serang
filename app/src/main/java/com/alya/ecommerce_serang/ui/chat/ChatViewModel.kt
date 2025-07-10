package com.alya.ecommerce_serang.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.chat.ChatItem
import com.alya.ecommerce_serang.data.api.response.chat.ChatItemList
import com.alya.ecommerce_serang.data.api.response.chat.ChatLine
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.data.repository.ChatRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.utils.Constants
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * ChatViewModel - Manages chat functionality for both buyers and store owners
 *
 * ARCHITECTURE OVERVIEW:
 * - Handles real-time messaging via Socket.IO
 * - Manages chat state using LiveData/MutableLiveData pattern
 * - Supports multiple message types: TEXT, IMAGE, PRODUCT
 * - Maintains separate flows for buyer and store owner chat
 *
 * KEY RESPONSIBILITIES:
 * 1. Socket connection management and real-time message handling
 * 2. Message sending/receiving with different attachment types
 * 3. Chat history loading and message status updates
 * 4. Product attachment functionality for commerce integration
 * 5. User session management and authentication
 *
 * STATE MANAGEMENT PATTERN:
 * - All UI state updates go through updateState() helper function
 * - State updates are atomic and follow immutable pattern
 * - Error states are cleared explicitly via clearError()
 */

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socketService: SocketIOService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "ChatViewModel"
    // Product attachment flag
    private var shouldAttachProduct = false

    // UI state using LiveData
    private val _state = MutableLiveData(ChatUiState())
    val state: LiveData<ChatUiState> = _state

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val _chatRoomId = MutableLiveData<Int>(0)
    val chatRoomId: LiveData<Int> = _chatRoomId

    private val _chatList = MutableLiveData<Result<List<ChatItemList>>>()
    val chatList: LiveData<Result<List<ChatItemList>>> = _chatList

    private val _chatListStore = MutableLiveData<Result<List<ChatItemList>>>()
    val chatListStore: LiveData<Result<List<ChatItemList>>> = _chatListStore

    private val _storeDetail = MutableLiveData<Result<List<StoreItem?>>>()
    val storeDetail: LiveData<Result<List<StoreItem?>>> get() = _storeDetail

    // Chat parameters
    private var storeId: Int = 0
    private var productId: Int = 0
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
        Log.d(TAG, "ChatViewModel initialized")
        initializeUser()
    }

    private fun initializeUser() {
        _isLoading.value = true
        viewModelScope.launch {
            Log.d(TAG, "Initializing user session...")

            when (val result = chatRepository.fetchUserProfile()) {
                is Result.Success -> {
                    currentUserId = result.data?.userId
                    _isLoading.value = false

                    Log.d(TAG, "User session initialized - User ID: $currentUserId")

                    if (currentUserId == null || currentUserId == 0) {
                        Log.e(TAG, "Invalid user ID detected")
                        updateState { it.copy(error = "User authentication error. Please login again.") }
                    } else {
                        Log.d(TAG, "Setting up socket listeners...")
                        setupSocketListeners()
                    }
                }
                is Result.Error -> {
                    _isLoading.value = false
                    Log.e(TAG, "Failed to fetch user profile: ${result.exception.message}")
                    updateState { it.copy(error = "User authentication error. Please login again.") }
                }
                is Result.Loading -> {
                    _isLoading.value = true
                    Log.d(TAG, "Loading user profile...")
                }
            }
        }
    }

    // set chat parameter for buyer
    fun setChatParameters(
        storeId: Int,
        productId: Int? = 0,
        productName: String? = null,
        productPrice: String? = null,
        productImage: String? = null,
        productRating: Float? = 0f,
        storeName: String
    ) {
        Log.d(TAG, "Setting chat parameters - StoreID: $storeId, ProductID: $productId")

        this.productId = if (productId != null && productId > 0) productId else 0
        this.storeId = storeId
        this.productName = productName.toString()
        this.productPrice = productPrice.toString()
        this.productImage = productImage.toString()
        this.productRating = productRating!!
        this.storeName = storeName

        updateState {
            it.copy(
                productName = productName.toString(),
                productPrice = productPrice.toString(),
                productImageUrl = productImage.toString(),
                productRating = productRating,
                storeName = storeName
            )
        }

        val existingChatRoomId = _chatRoomId.value ?: 0
        if (existingChatRoomId > 0) {
            Log.d(TAG, "Loading existing chat room: $existingChatRoomId")
            loadChatHistory(existingChatRoomId)
            joinSocketRoom(existingChatRoomId)
        }
    }

    // set chat parameter for store
    fun setChatParametersStore(
        storeId: Int,
        userId: Int,
        productId: Int? = 0,
        productName: String? = null,
        productPrice: String? = null,
        productImage: String? = null,
        productRating: Float? = 0f,
        storeName: String
    ) {
        Log.d(TAG, "Setting store chat parameters - StoreID: $storeId, UserID: $userId, ProductID: $productId")

        this.productId = if (productId != null && productId > 0) productId else 0
        this.storeId = storeId
        this.defaultUserId = userId
        this.productName = productName.toString()
        this.productPrice = productPrice.toString()
        this.productImage = productImage.toString()
        this.productRating = productRating!!
        this.storeName = storeName

        updateState {
            it.copy(
                productName = productName.toString(),
                productPrice = productPrice.toString(),
                productImageUrl = productImage.toString(),
                productRating = productRating,
                storeName = storeName
            )
        }

        val existingChatRoomId = _chatRoomId.value ?: 0
        if (existingChatRoomId > 0) {
            Log.d(TAG, "Loading existing store chat room: $existingChatRoomId")
            loadChatHistory(existingChatRoomId)
            joinSocketRoom(existingChatRoomId)
        }
    }

    //enable product attach from detailproductactivity
    fun enableProductAttachment() {
        Log.d(TAG, "Product attachment enabled - ProductID: $productId, ProductName: $productName")
        shouldAttachProduct = true
        updateState { it.copy(hasProductAttachment = true) }
    }

    // disable product attach
    fun disableProductAttachment() {
        Log.d(TAG, "Product attachment disabled")
        shouldAttachProduct = false
        updateState { it.copy(hasProductAttachment = false) }
    }

    private fun setupSocketListeners() {
        Log.d(TAG, "Setting up socket listeners...")

        viewModelScope.launch {
            socketService.connectionState.collect { connectionState ->
                Log.d(TAG, "Socket connection state changed: $connectionState")
                updateState { it.copy(connectionState = connectionState) }

                if (connectionState is ConnectionState.Connected) {
                    Log.d(TAG, "Socket connected, joining room...")
                    socketService.joinRoom()
                }
            }
        }

        viewModelScope.launch {
            socketService.newMessages.collect { chatLine ->
                chatLine?.let {
                    Log.d(TAG, "New message received via socket - ID: ${it.id}, SenderID: ${it.senderId}")
                    val currentMessages = _state.value?.messages ?: listOf()
                    val updatedMessages = currentMessages.toMutableList().apply {
                        add(convertChatLineToUiMessage(it))
                    }
                    updateState { it.copy(messages = updatedMessages) }

                    if (it.senderId != currentUserId) {
                        Log.d(TAG, "Marking message as read: ${it.id}")
                        updateMessageStatus(it.id, Constants.STATUS_READ)
                    }
                }
            }
        }

        viewModelScope.launch {
            socketService.typingStatus.collect { typingStatus ->
                typingStatus?.let {
                    val currentRoomId = _chatRoomId.value ?: 0
                    if (typingStatus.roomId == currentRoomId && typingStatus.userId != currentUserId) {
                        Log.d(TAG, "Typing status updated: ${typingStatus.isTyping}")
                        updateState { it.copy(isOtherUserTyping = typingStatus.isTyping) }
                    }
                }
            }
        }
    }

    fun joinSocketRoom(roomId: Int) {
        if (roomId <= 0) {
            Log.e(TAG, "Cannot join room: Invalid room ID")
            return
        }

        Log.d(TAG, "Joining socket room: $roomId")
        socketService.joinRoom()
    }

    fun sendTypingStatus(isTyping: Boolean) {
        val roomId = _chatRoomId.value ?: 0
        if (roomId <= 0) {
            Log.w(TAG, "Cannot send typing status: No active room")
            return
        }

        Log.d(TAG, "Sending typing status: $isTyping for room: $roomId")
        socketService.sendTypingStatus(roomId, isTyping)
    }

    // load chat history
    fun loadChatHistory(chatRoomId: Int) {
        if (chatRoomId <= 0) {
            Log.e(TAG, "Cannot load chat history: Invalid chat room ID")
            return
        }

        Log.d(TAG, "Loading chat history for room: $chatRoomId")

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            when (val result = chatRepository.getChatHistory(chatRoomId)) {
                is Result.Success -> {
                    Log.d(TAG, "Chat history loaded successfully - ${result.data.chat.size} messages")

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

                    // Update status of unread messages
                    val unreadMessages = result.data.chat.filter {
                        it.senderId != currentUserId && it.status != Constants.STATUS_READ
                    }

                    if (unreadMessages.isNotEmpty()) {
                        Log.d(TAG, "Marking ${unreadMessages.size} messages as read")
                        unreadMessages.forEach { updateMessageStatus(it.id, Constants.STATUS_READ) }
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error loading chat history: ${result.exception.message}")
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                }
                is Result.Loading -> {
                    updateState { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun getChatList() {
        _isLoading.value = true
        Log.d(TAG, "Getting chat list...")
        viewModelScope.launch {
//            _chatList.value = Result.Loading
            _chatList.value = chatRepository.getListChat()
            _isLoading.value = false

        }
    }

    fun getChatListStore() {
        Log.d(TAG, "Getting store chat list...")
        _chatListStore.value = Result.Loading

        viewModelScope.launch {
            val result = chatRepository.getListChatStore()
            Log.d(TAG, "Store chat list result: $result")
            _chatListStore.value = result
        }
    }

    // handle regular message and product message
    fun sendMessage(message: String) {
        Log.d(TAG, "=== SEND MESSAGE ===")
        Log.d(TAG, "Message: '$message'")
        Log.d(TAG, "Should attach product: $shouldAttachProduct")
        Log.d(TAG, "Has image attachment: ${selectedImageFile != null}")
        Log.d(TAG, "Product ID: $productId")

        if (message.isBlank() && selectedImageFile == null) {
            Log.w(TAG, "Cannot send message: Both message and image are empty")
            return
        }

        if (storeId <= 0) {
            Log.e(TAG, "Cannot send message: Invalid store ID")
            updateState { it.copy(error = "Cannot send message. Invalid store ID.") }
            return
        }

        // Check for product attachment
        if (shouldAttachProduct && productId > 0) {
            Log.d(TAG, "Sending message with product attachment")
            sendMessageWithProduct(message)
            shouldAttachProduct = false
            updateState { it.copy(hasProductAttachment = false) }
            return
        }

        Log.d(TAG, "Sending regular message")
        sendRegularMessage(message)
    }

    //send message for store
    fun sendMessageStore(message: String) {
        Log.d(TAG, "=== SEND MESSAGE STORE ===")
        Log.d(TAG, "Message: '$message'")
        Log.d(TAG, "Has attachment: ${selectedImageFile != null}")
        Log.d(TAG, "Default User ID: $defaultUserId")

        if (message.isBlank() && selectedImageFile == null) {
            Log.w(TAG, "Cannot send store message: Both message and image are empty")
            return
        }

        if (storeId <= 0) {
            Log.e(TAG, "Cannot send store message: Invalid store ID")
            updateState { it.copy(error = "Cannot send message. Invalid store ID.") }
            return
        }

        if (defaultUserId <= 0) {
            Log.e(TAG, "Cannot send store message: Invalid user ID")
            updateState { it.copy(error = "Cannot send message. Invalid user ID.") }
            return
        }

        selectedImageFile?.let { file ->
            if (file.exists() && file.length() > 5 * 1024 * 1024) {
                Log.e(TAG, "Image file too large: ${file.length()} bytes")
                updateState { it.copy(error = "Image file is too large. Please select a smaller image.") }
                return
            }
        }

        val existingChatRoomId = _chatRoomId.value ?: 0
        Log.d(TAG, "Sending store message - StoreID: $storeId, UserID: $defaultUserId, RoomID: $existingChatRoomId")

        viewModelScope.launch {
            updateState { it.copy(isSending = true) }

            try {
                val safeProductId = if (productId == 0) null else productId

                val result = chatRepository.sendChatMessageStore(
                    userId = defaultUserId,
                    message = message,
                    productId = safeProductId,
                    imageFile = selectedImageFile
                )

                when (result) {
                    is Result.Success -> {
                        val chatLine = result.data.chatLine
                        Log.d(TAG, "Store message sent successfully - ID: ${chatLine.id}")

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

                        handleChatRoomCreation(existingChatRoomId, chatLine.chatRoomId)
                        socketService.sendMessage(chatLine)
                        selectedImageFile = null
                    }
                    is Result.Error -> {
                        val errorMsg = result.exception.message?.takeIf { it.isNotBlank() && it != "{}" }
                            ?: "Failed to send message. Please try again."

                        Log.e(TAG, "Error sending store message: $errorMsg")
                        updateState {
                            it.copy(
                                isSending = false,
                                error = errorMsg
                            )
                        }
                    }
                    is Result.Loading -> {
                        updateState { it.copy(isSending = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendMessageStore", e)
                updateState {
                    it.copy(
                        isSending = false,
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    // send message with product info
    private fun sendMessageWithProduct(userMessage: String) {
        Log.d(TAG, "Sending message with product attachment")
        Log.d(TAG, "User message: '$userMessage'")
        Log.d(TAG, "Product: $productName")

        // Send product bubble FIRST
        sendProductBubble()

        // Then send user's text message after a small delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            sendTextMessage(userMessage)
        }
    }

    // send only text message w/o product info
    private fun sendTextMessage(message: String) {
        if (message.isBlank()) {
            Log.w(TAG, "Cannot send text message: Message is blank")
            return
        }

        Log.d(TAG, "Sending text message: '$message'")

        viewModelScope.launch {
            updateState { it.copy(isSending = true) }

            try {
                val result = chatRepository.sendChatMessage(
                    storeId = storeId,
                    message = message,
                    productId = null,
                    imageFile = selectedImageFile
                )

                when (result) {
                    is Result.Success -> {
                        val chatLine = result.data.chatLine
                        Log.d(TAG, "Text message sent successfully - ID: ${chatLine.id}")

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

                        val existingChatRoomId = _chatRoomId.value ?: 0
                        handleChatRoomCreation(existingChatRoomId, chatLine.chatRoomId)
                        socketService.sendMessage(chatLine)
                        selectedImageFile = null
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error sending text message: ${result.exception.message}")
                        updateState {
                            it.copy(
                                isSending = false,
                                error = result.exception.message ?: "Failed to send message"
                            )
                        }
                    }
                    is Result.Loading -> {
                        updateState { it.copy(isSending = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendTextMessage", e)
                updateState {
                    it.copy(
                        isSending = false,
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    // send product bubble message
    private fun sendProductBubble() {
        Log.d(TAG, "Sending product bubble - ProductID: $productId, ProductName: $productName")

        viewModelScope.launch {
            try {
                val result = chatRepository.sendChatMessage(
                    storeId = storeId,
                    message = "",
                    productId = productId,
                    imageFile = null
                )

                when (result) {
                    is Result.Success -> {
                        val chatLine = result.data.chatLine
                        Log.d(TAG, "Product bubble sent successfully - ID: ${chatLine.id}")

                        val newMessage = convertChatLineToUiMessage(chatLine).copy(
                            messageType = MessageType.PRODUCT,
                            productInfo = ProductInfo(
                                productId = productId,
                                productName = productName,
                                productPrice = productPrice,
                                productImage = productImage,
                                productRating = productRating,
                                storeName = storeName
                            )
                        )

                        val currentMessages = _state.value?.messages ?: listOf()
                        val updatedMessages = currentMessages.toMutableList().apply {
                            add(newMessage)
                        }

                        updateState {
                            it.copy(
                                messages = updatedMessages,
                                error = null
                            )
                        }

                        socketService.sendMessage(chatLine)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error sending product bubble: ${result.exception.message}")
                        updateState {
                            it.copy(
                                error = "Failed to send product info: ${result.exception.message}"
                            )
                        }
                    }
                    is Result.Loading -> {
                        // Handle loading if needed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendProductBubble", e)
                updateState {
                    it.copy(
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    // send regular message for normal chat
    private fun sendRegularMessage(message: String) {
        Log.d(TAG, "Sending regular message: '$message'")

        selectedImageFile?.let { file ->
            if (file.exists() && file.length() > 5 * 1024 * 1024) {
                Log.e(TAG, "Image file too large: ${file.length()} bytes")
                updateState { it.copy(error = "Image file is too large. Please select a smaller image.") }
                return
            }
        }

        val existingChatRoomId = _chatRoomId.value ?: 0

        viewModelScope.launch {
            updateState { it.copy(isSending = true) }

            try {
                val safeProductId = if (productId == 0) null else productId

                val result = chatRepository.sendChatMessage(
                    storeId = storeId,
                    message = message,
                    productId = safeProductId,
                    imageFile = selectedImageFile
                )

                when (result) {
                    is Result.Success -> {
                        val chatLine = result.data.chatLine
                        Log.d(TAG, "Regular message sent successfully - ID: ${chatLine.id}")

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

                        handleChatRoomCreation(existingChatRoomId, chatLine.chatRoomId)
                        socketService.sendMessage(chatLine)
                        selectedImageFile = null
                    }
                    is Result.Error -> {
                        val errorMsg = result.exception.message?.takeIf { it.isNotBlank() && it != "{}" }
                            ?: "Failed to send message. Please try again."

                        Log.e(TAG, "Error sending regular message: $errorMsg")
                        updateState {
                            it.copy(
                                isSending = false,
                                error = errorMsg
                            )
                        }
                    }
                    is Result.Loading -> {
                        updateState { it.copy(isSending = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendRegularMessage", e)
                updateState {
                    it.copy(
                        isSending = false,
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

   //update message status
    fun updateMessageStatus(messageId: Int, status: String) {
        Log.d(TAG, "Updating message status - ID: $messageId, Status: $status")

        viewModelScope.launch {
            try {
                val result = chatRepository.updateMessageStatus(messageId, status)

                if (result is Result.Success) {
                    val currentMessages = _state.value?.messages ?: listOf()
                    val updatedMessages = currentMessages.map { message ->
                        if (message.id == messageId) {
                            message.copy(status = status)
                        } else {
                            message
                        }
                    }
                    updateState { it.copy(messages = updatedMessages) }
                    Log.d(TAG, "Message status updated successfully")
                } else if (result is Result.Error) {
                    Log.e(TAG, "Error updating message status: ${result.exception.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating message status", e)
            }
        }
    }

   //set image attachment
    fun setSelectedImageFile(file: File?) {
        selectedImageFile = file
        updateState { it.copy(hasAttachment = file != null) }
        Log.d(TAG, "Image attachment ${if (file != null) "selected: ${file.name}" else "cleared"}")
    }

    fun clearSelectedImage() {
        Log.d(TAG, "Clearing selected image attachment")

        selectedImageFile?.let { file ->
            Log.d(TAG, "Clearing image file: ${file.name}")
        }

        selectedImageFile = null
        updateState { it.copy(hasAttachment = false) }

        Log.d(TAG, "Image attachment cleared successfully")
    }

    // convert form chatLine api to UI chat messages
    private fun convertChatLineToUiMessage(chatLine: ChatLine): ChatUiMessage {
        val formattedTime = formatTimestamp(chatLine.createdAt)

        return ChatUiMessage(
            id = chatLine.id,
            message = chatLine.message,
            attachment = chatLine.attachment ?: "",
            status = chatLine.status,
            time = formattedTime,
            isSentByMe = chatLine.senderId == currentUserId,
            createdAt = chatLine.createdAt
        )
    }

   // convert chat history item to ui
    private fun convertChatLineToUiMessageHistory(chatItem: ChatItem): ChatUiMessage {
        val formattedTime = formatTimestamp(chatItem.createdAt)

        val messageType = when {
            chatItem.productId > 0 -> MessageType.PRODUCT
            !chatItem.attachment.isNullOrEmpty() -> MessageType.IMAGE
            else -> MessageType.TEXT
        }

        val productInfo = if (messageType == MessageType.PRODUCT) {
            if (chatItem.senderId == currentUserId && chatItem.productId == productId) {
                ProductInfo(
                    productId = productId,
                    productName = productName,
                    productPrice = productPrice,
                    productImage = productImage,
                    productRating = productRating,
                    storeName = storeName
                )
            } else {
                ProductInfo(
                    productId = chatItem.productId,
                    productName = "Loading...",
                    productPrice = "Loading...",
                    productImage = "",
                    productRating = 0f,
                    storeName = "Loading..."
                )
            }
        } else null

        val message = ChatUiMessage(
            id = chatItem.id,
            message = chatItem.message,
            attachment = chatItem.attachment,
            status = chatItem.status,
            time = formattedTime,
            isSentByMe = chatItem.senderId == currentUserId,
            messageType = messageType,
            productInfo = productInfo,
            createdAt = chatItem.createdAt
        )

        // Fetch product info for non-current-user products
        if (messageType == MessageType.PRODUCT &&
            (chatItem.senderId != currentUserId || chatItem.productId != productId)) {
            fetchProductInfoForHistoryMessage(message, chatItem.productId)
        }

        return message
    }

    // fetch produc =t info in chat history
    private fun fetchProductInfoForHistoryMessage(message: ChatUiMessage, productId: Int) {
        Log.d(TAG, "Fetching product info for message ${message.id}, productId: $productId")

        viewModelScope.launch {
            try {
                val productResult = chatRepository.fetchProductDetail(productId)

                if (productResult != null) {
                    val product = productResult.product
                    Log.d(TAG, "Product fetched successfully: ${product.productName}")

                    val productInfo = ProductInfo(
                        productId = product.productId,
                        productName = product.productName,
                        productPrice = formatPrice(product.price),
                        productImage = product.image,
                        productRating = parseRating(product.rating),
                        storeName = getStoreName(product.storeId)
                    )

                    updateMessageWithProductInfo(message.id, productInfo)
                } else {
                    Log.e(TAG, "Failed to fetch product info for productId: $productId")
                    val errorProductInfo = ProductInfo(
                        productId = productId,
                        productName = "Product not available",
                        productPrice = "N/A",
                        productImage = "",
                        productRating = 0f,
                        storeName = "Unknown Store"
                    )
                    updateMessageWithProductInfo(message.id, errorProductInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching product info for message: ${message.id}", e)
                val errorProductInfo = ProductInfo(
                    productId = productId,
                    productName = "Error loading product",
                    productPrice = "N/A",
                    productImage = "",
                    productRating = 0f,
                    storeName = "Unknown Store"
                )
                updateMessageWithProductInfo(message.id, errorProductInfo)
            }
        }
    }

    //update specific message with product info
    private fun updateMessageWithProductInfo(messageId: Int, productInfo: ProductInfo) {
        Log.d(TAG, "Updating message $messageId with product info: ${productInfo.productName}")

        val currentMessages = _state.value?.messages ?: listOf()
        val updatedMessages = currentMessages.map { message ->
            if (message.id == messageId) {
                message.copy(productInfo = productInfo)
            } else {
                message
            }
        }
        updateState { it.copy(messages = updatedMessages) }
    }

    //handle chat room when initiate chat
    private fun handleChatRoomCreation(existingChatRoomId: Int, newChatRoomId: Int) {
        if (existingChatRoomId == 0 && newChatRoomId > 0) {
            Log.d(TAG, "Chat room created: $newChatRoomId")
            _chatRoomId.value = newChatRoomId
            joinSocketRoom(newChatRoomId)
        }
    }

    //format timestamp
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = inputFormat.parse(timestamp)
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $timestamp", e)
            ""
        }
    }

   //format price
    private fun formatPrice(price: String): String {
        return if (price.startsWith("Rp")) price else "Rp$price"
    }

    // parse string to float
    private fun parseRating(rating: String): Float {
        return try {
            rating.toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing rating: $rating", e)
            0f
        }
    }

    //get store name by Id
    private fun getStoreName(storeId: Int): String {
        return if (storeId == this.storeId) {
            storeName
        } else {
            "Store #$storeId"
        }
    }

    // helper function to update live data
    private fun updateState(update: (ChatUiState) -> ChatUiState) {
        _state.value?.let {
            _state.value = update(it)
        }
    }

    //clear any error messages
    fun clearError() {
        Log.d(TAG, "Clearing error state")
        updateState { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel cleared - Disconnecting socket")
        socketService.disconnect()
    }

    fun getDisplayItems(): List<ChatDisplayItem> {
        return transformMessagesToDisplayItems(state.value?.messages ?: emptyList())
    }

    private fun transformMessagesToDisplayItems(messages: List<ChatUiMessage>): List<ChatDisplayItem> {
        if (messages.isEmpty()) return emptyList()

        val displayItems = mutableListOf<ChatDisplayItem>()
        var lastDate: String? = null

        for (message in messages) {
            // Extract date from message timestamp
            val messageDate = extractDateFromTimestamp(message.createdAt) // You need to implement this

            // Add date header if this is a new day
            if (messageDate != lastDate) {
                val formattedDate = formatDateHeader(messageDate) // You need to implement this
                displayItems.add(ChatDisplayItem.DateHeaderItem(messageDate, formattedDate))
                lastDate = messageDate
            }

            // Add the message
            displayItems.add(ChatDisplayItem.MessageItem(message))
        }

        return displayItems
    }

    private fun extractDateFromTimestamp(timestamp: String): String {
        return try {
            // Parse ISO 8601 format: "2025-05-27T08:36:53.946Z"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(timestamp)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timestamp: $timestamp", e)
            return timestamp.take(10)
        }
    }

    private fun formatDateHeader(dateString: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val messageDate = dateFormat.parse(dateString) ?: return dateString

            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val messageCalendar = Calendar.getInstance().apply { time = messageDate }

            when {
                isSameDay(messageCalendar, today) -> "Today"
                isSameDay(messageCalendar, yesterday) -> "Yesterday"
                isThisYear(messageCalendar, today) -> {
                    // Show "Mon, Dec 15" format for this year
                    SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(messageDate)
                }
                else -> {
                    // Show "Dec 15, 2024" format for other years
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(messageDate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateString", e)
            dateString // Fallback to raw date
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisYear(messageCalendar: Calendar, today: Calendar): Boolean {
        return messageCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }
}

enum class MessageType {
    TEXT,           // Regular text message
    IMAGE,          // Image message
    PRODUCT         // Product share message
}

data class ProductInfo(
    val productId: Int,
    val productName: String,
    val productPrice: String,
    val productImage: String,
    val productRating: Float,
    val storeName: String
)

// representing chat messages to UI
data class ChatUiMessage(
    val id: Int,
    val message: String,
    val attachment: String?,
    val status: String,
    val time: String,
    val isSentByMe: Boolean,
    val messageType: MessageType = MessageType.TEXT,
    val productInfo: ProductInfo? = null,
    val createdAt: String
)



// representing UI state to screen
data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val hasAttachment: Boolean = false,
    val hasProductAttachment: Boolean = false,
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