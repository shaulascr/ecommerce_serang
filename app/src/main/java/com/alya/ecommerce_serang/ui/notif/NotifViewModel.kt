package com.alya.ecommerce_serang.ui.notif

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.auth.HasStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.NotifItem
import com.alya.ecommerce_serang.data.api.response.auth.NotifstoreItem
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager

) : ViewModel() {

    private val _notifList = MutableLiveData<Result<List<NotifItem>>>()
    val notifList: LiveData<Result<List<NotifItem>>> = _notifList

    private val _checkStore = MutableLiveData<Boolean>()
    val checkStore: LiveData<Boolean> = _checkStore

    private val _notifStoreList = MutableLiveData<Result<List<NotifstoreItem>>>()
    val notifStoreList: LiveData<Result<List<NotifstoreItem>>> = _notifStoreList

    fun getNotifList() {
        Log.d(TAG, "getNotifList: Fetching personal notifications")
        viewModelScope.launch {
            try {
                Log.d(TAG, "getNotifList: Setting state to Loading")
                _notifList.value = Result.Loading

                Log.d(TAG, "getNotifList: Calling repository to get notifications")
                val result = userRepository.getListNotif()

                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "getNotifList: Success, received ${result.data?.size ?: 0} notifications")
                        if (result.data != null && result.data.isNotEmpty()) {
                            Log.d(TAG, "getNotifList: First notification - id: ${result.data[0].id}, title: ${result.data[0].title}")
                            if (result.data.size > 1) {
                                Log.d(TAG, "getNotifList: Last notification - id: ${result.data[result.data.size-1].id}, title: ${result.data[result.data.size-1].title}")
                            }
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "getNotifList: Error fetching notifications", result.exception)
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "getNotifList: State is Loading")
                    }
                }

                _notifList.value = result
            } catch (e: Exception) {
                Log.e(TAG, "getNotifList: Unexpected error", e)
                _notifList.value = Result.Error(e)
            }
        }
    }

    fun getNotifStoreList() {
        Log.d(TAG, "getNotifStoreList: Fetching store notifications")
        viewModelScope.launch {
            try {
                Log.d(TAG, "getNotifStoreList: Setting state to Loading")
                _notifStoreList.value = Result.Loading

                Log.d(TAG, "getNotifStoreList: Calling repository to get store notifications")
                val result = userRepository.getListNotifStore()

                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "getNotifStoreList: Success, received ${result.data?.size ?: 0} store notifications")
                        if (result.data != null && result.data.isNotEmpty()) {
                            Log.d(TAG, "getNotifStoreList: First store notification - id: ${result.data[0].id}, title: ${result.data[0].title}")
                            if (result.data.size > 1) {
                                Log.d(TAG, "getNotifStoreList: Last store notification - id: ${result.data[result.data.size-1].id}, title: ${result.data[result.data.size-1].title}")
                            }
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "getNotifStoreList: Error fetching store notifications", result.exception)
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "getNotifStoreList: State is Loading")
                    }
                }

                _notifStoreList.value = result
            } catch (e: Exception) {
                Log.e(TAG, "getNotifStoreList: Unexpected error", e)
                _notifStoreList.value = Result.Error(e)
            }
        }
    }

    fun checkStoreUser() {
        Log.d(TAG, "checkStoreUser: Checking if user has a store")
        viewModelScope.launch {
            try {
                // Call the repository function to check store
                Log.d(TAG, "checkStoreUser: Calling repository to check store")
                val response: HasStoreResponse = userRepository.checkStore()

                // Log and store success message
                Log.d(TAG, "checkStoreUser: Response received, hasStore=${response.hasStore}")
                _checkStore.value = response.hasStore // Store the value for UI feedback
                Log.d(TAG, "checkStoreUser: Updated _checkStore value to ${response.hasStore}")

            } catch (exception: Exception) {
                // Handle any errors and update state
                Log.e(TAG, "checkStoreUser: Error checking store", exception)
                _checkStore.value = false
                Log.d(TAG, "checkStoreUser: Set _checkStore to false due to error")
            }
        }
    }

    companion object {
        private const val TAG = "NotifViewModel" // Constant for logging tag

    }

}