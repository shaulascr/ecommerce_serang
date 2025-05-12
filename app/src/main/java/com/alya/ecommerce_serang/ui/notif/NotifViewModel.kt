package com.alya.ecommerce_serang.ui.notif

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifViewModel @Inject constructor(
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManagerCompat,
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val webSocketManager: WebSocketManager,
    private val sessionManager: SessionManager

) : ViewModel() {

    private val _userProfile = MutableStateFlow<Result<UserProfile?>>(Result.Loading)
    val userProfile: StateFlow<Result<UserProfile?>> = _userProfile.asStateFlow()

    init {
        fetchUserProfile()
    }

    // Fetch user profile to get necessary data
    fun fetchUserProfile() {
        viewModelScope.launch {
            _userProfile.value = Result.Loading
            val result = userRepository.fetchUserProfile()
            _userProfile.value = result

            // If successful, save the user ID for WebSocket use
            if (result is Result.Success && result.data != null) {
                sessionManager.saveUserId(result.data.userId.toString())
            }
        }
    }

    // Start WebSocket connection
    fun startWebSocketConnection() {
        webSocketManager.startWebSocketConnection()
    }

    // Stop WebSocket connection
    fun stopWebSocketConnection() {
        webSocketManager.stopWebSocketConnection()
    }

    // Call when ViewModel is cleared (e.g., app closing)
    override fun onCleared() {
        super.onCleared()
        // No need to stop here - the service will manage its own lifecycle
    }
}