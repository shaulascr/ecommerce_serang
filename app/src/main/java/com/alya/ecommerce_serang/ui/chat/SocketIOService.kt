package com.alya.ecommerce_serang.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alya.ecommerce_serang.BuildConfig
import com.alya.ecommerce_serang.data.api.response.chat.ChatLine
import com.alya.ecommerce_serang.utils.Constants
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketIOService @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "SocketIOService"

    // Socket.IO client
    private var socket: Socket? = null

    // Connection state
    private var isConnected = false

    // StateFlows for internal observing (these are needed for suspend functions in ViewModel)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _newMessages = MutableSharedFlow<ChatLine>(extraBufferCapacity = 1) // Using extraBufferCapacity for a non-suspending emit
    val newMessages: SharedFlow<ChatLine> = _newMessages

    private val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus

    // LiveData for Activity/Fragment observing
    private val _connectionStateLiveData = MutableLiveData<ConnectionState>(ConnectionState.Disconnected())
    val connectionStateLiveData: LiveData<ConnectionState> = _connectionStateLiveData

    private val _newMessagesLiveData = MutableLiveData<ChatLine?>()
    val newMessagesLiveData: LiveData<ChatLine?> = _newMessagesLiveData

    private val _typingStatusLiveData = MutableLiveData<TypingStatus?>()
    val typingStatusLiveData: LiveData<TypingStatus?> = _typingStatusLiveData

    /**
     * Initializes the Socket.IO client
     */
    init {
        try {
            // Get token from SessionManager
            val token = sessionManager.getToken()

            // Set up Socket.IO options with auth token
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 3000

                // Add auth information
                if (!token.isNullOrEmpty()) {
                    auth = mapOf("token" to token)
                }
            }

            // Create Socket.IO client
            socket = IO.socket(BuildConfig.BASE_URL, options)

            // Set up event listeners
            setupSocketListeners()

            Log.d(TAG, "Socket.IO initialized with token: $token")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Error initializing Socket.IO client", e)
            _connectionState.value = ConnectionState.Error("Error initializing Socket.IO: ${e.message}")
            _connectionStateLiveData.value = ConnectionState.Error("Error initializing Socket.IO: ${e.message}")
        }
    }

    /**
     * Sets up Socket.IO event listeners
     */
    private fun setupSocketListeners() {

        socket?.on(Constants.EVENT_NEW_MESSAGE) { args -> // Use the event name your server emits
            Log.d(TAG, "Raw event received on ${Constants.EVENT_NEW_MESSAGE}: ${args.firstOrNull()}") // Check raw args

            if (args.isNotEmpty()) {
                try {
                    val messageJson = args[0].toString()
                    val chatLine = Gson().fromJson(messageJson, ChatLine::class.java)
                    Log.d(TAG, "Successfully parsed ChatLine: ${chatLine.message}")
                    Log.d(TAG, "Emitting new message to _newMessages SharedFlow...") // New log

                    // Use the serviceScope to launch a coroutine for emit()
                    serviceScope.launch {
                        _newMessages.emit(chatLine) // This ensures every message is processed
                        Log.d(TAG, "New message emitted to SharedFlow.") // New log after emit

                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing or emitting new message: ${e.message}", e)
                }
            } else {
            Log.w(TAG, "Received empty args for ${Constants.EVENT_NEW_MESSAGE}")
            }
        }
//        socket?.on(Constants.EVENT_NEW_MESSAGE) { args ->
//            if (args.isNotEmpty()) {
//                val messageJson = args[0].toString()
//                val chatLine = Gson().fromJson(messageJson, ChatLine::class.java)
//                Log.d("SocketIOService", "Message received: ${chatLine.message}")
//                _newMessages.value = chatLine
//            }
//        }
//        socket?.let { socket ->
//            // Connection events
//            socket.on(Socket.EVENT_CONNECT) {
//                Log.d(TAG, "Socket.IO connected")
//                isConnected = true
//                _connectionState.value = ConnectionState.Connected
//                _connectionStateLiveData.postValue(ConnectionState.Connected)
//            }
//
//            socket.on(Socket.EVENT_DISCONNECT) {
//                Log.d(TAG, "Socket.IO disconnected")
//                isConnected = false
//                _connectionState.value = ConnectionState.Disconnected("Disconnected from server")
//                _connectionStateLiveData.postValue(ConnectionState.Disconnected("Disconnected from server"))
//            }
//
//            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
//                val error = if (args.isNotEmpty() && args[0] != null) args[0].toString() else "Unknown error"
//                Log.e(TAG, "Socket.IO connection error: $error")
//                isConnected = false
//                _connectionState.value = ConnectionState.Error("Connection error: $error")
//                _connectionStateLiveData.postValue(ConnectionState.Error("Connection error: $error"))
//            }
//
//            // Chat events
//            socket.on(Constants.EVENT_NEW_MESSAGE) { args ->
//                try {
//                    if (args.isNotEmpty() && args[0] != null) {
//                        val messageJson = args[0].toString()
//                        Log.d(TAG, "Received new message: $messageJson")
//                        val chatLine = Gson().fromJson(messageJson, ChatLine::class.java)
//                        _newMessages.value = chatLine
//                        _newMessagesLiveData.postValue(chatLine)
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing new message event", e)
//                }
//            }
//
//            socket.on(Constants.EVENT_TYPING) { args ->
//                try {
//                    if (args.isNotEmpty() && args[0] != null) {
//                        val typingData = args[0] as JSONObject
//                        val userId = typingData.getInt("userId")
//                        val roomId = typingData.getInt("roomId")
//                        val isTyping = typingData.getBoolean("isTyping")
//
//                        Log.d(TAG, "Received typing status: User $userId in room $roomId is typing: $isTyping")
//                        val status = TypingStatus(userId, roomId, isTyping)
//                        _typingStatus.value = status
//                        _typingStatusLiveData.postValue(status)
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing typing event", e)
//                }
//            }
//        }
    }

    /**
     * Connects to the Socket.IO server
     */
    fun connect() {
        if (isConnected) return

        Log.d(TAG, "Connecting to Socket.IO server...")
        _connectionState.value = ConnectionState.Connecting
        _connectionStateLiveData.value = ConnectionState.Connecting
        socket?.connect()
    }

    /**
     * Joins a specific chat room
     */
    fun joinRoom(roomId: Int) {
//        if (!isConnected) {
//            connect()
//            return
//        }
//
//        // Get user ID from SessionManager
//        val userId = sessionManager.getUserId()
//        if (userId.isNullOrEmpty()) {
//            Log.e(TAG, "Cannot join room: User ID is null or empty")
//            return
//        }
//
//        // Join the room using the current user's ID
//        socket?.emit("joinRoom", roomId) // ✅
//        Log.d(TAG, "Joined room ID: $roomId")
//        Log.d(TAG, "Joined room for user: $userId")
        if (!isConnected) {
            connect()
        }

        socket?.emit("joinRoom", roomId)
        Log.d(TAG, "Joined room ID: $roomId")
    }

    /**
     * Emits a new message event
     */
    fun sendMessage(message: ChatLine) {
        if (!isConnected) {
            connect()
            return
        }

        val messageJson = Gson().toJson(message)
        socket?.emit(Constants.EVENT_NEW_MESSAGE, messageJson)
        Log.d(TAG, "Sent message via Socket.IO: $messageJson")
    }

    /**
     * Sends typing status update
     */
    fun sendTypingStatus(roomId: Int, isTyping: Boolean) {
        if (!isConnected) return

        // Get user ID from SessionManager
        val userId = sessionManager.getUserId()?.toIntOrNull()
        if (userId == null) {
            Log.e(TAG, "Cannot send typing status: User ID is null or invalid")
            return
        }

        val typingData = JSONObject().apply {
            put("userId", userId)
            put("roomId", roomId)
            put("isTyping", isTyping)
        }

        socket?.emit(Constants.EVENT_TYPING, typingData)
        Log.d(TAG, "Sent typing status: User $userId in room $roomId is typing: $isTyping")
    }

    /**
     * Disconnects from the Socket.IO server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from Socket.IO server...")
        socket?.disconnect()
        isConnected = false
        _connectionState.value = ConnectionState.Disconnected("Disconnected by user")
        _connectionStateLiveData.postValue(ConnectionState.Disconnected("Disconnected by user"))
    }

    /**
     * Returns whether the socket is connected
     */
    val isSocketConnected: Boolean
        get() = isConnected
}

/**
 * Sealed class representing connection states
 */
sealed class ConnectionState {
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Disconnected(val reason: String = "") : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Data class for typing status events
 */
data class TypingStatus(
    val userId: Int,
    val roomId: Int,
    val isTyping: Boolean
)