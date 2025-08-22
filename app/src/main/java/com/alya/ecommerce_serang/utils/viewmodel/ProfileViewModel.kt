package com.alya.ecommerce_serang.utils.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.auth.ChangePassResponse
import com.alya.ecommerce_serang.data.api.response.auth.HasStoreResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.EditProfileResponse
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage : LiveData<String> = _errorMessage

    private val _editProfileResult = MutableLiveData<Result<EditProfileResponse>>()
    val editProfileResult: LiveData<Result<EditProfileResponse>> = _editProfileResult

    private val _checkStore = MutableLiveData<Boolean>()
    val checkStore: LiveData<Boolean> = _checkStore
    val changePasswordResult = MutableLiveData<Result<ChangePassResponse>>()
    private val _logout = MutableLiveData<Boolean>()
    val logout : LiveData<Boolean> = _logout

    fun loadUserProfile(){
        viewModelScope.launch {
            when (val result = userRepository.fetchUserProfile()){
                is Result.Success -> _userProfile.postValue(result.data)
                is Result.Error -> _errorMessage.postValue(result.exception.message ?: "Unknown Error")
                is Result.Loading -> null
            }
        }
    }

    fun checkStoreUser(){
        viewModelScope.launch {
            try {
                // Call the repository function to request OTP
                val response: HasStoreResponse = userRepository.checkStore()

                // Log and store success message
                Log.d("ProfileViewModel", "Has store: ${response.hasStore}")
                _checkStore.postValue(response.hasStore) // Store the message for UI feedback

            } catch (exception: Exception) {
                // Handle any errors and update state
                _checkStore.postValue(false)

                // Log the error for debugging
                Log.e(":ProfileViewModel", "Error:", exception)
            }
        }
    }



    fun editProfileDirect(
        context: Context,
        username: String,
        name: String,
        phone: String,
        birthDate: String,
        email: String,
        imageUri: Uri?
    ) {
        _editProfileResult.value = Result.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling editProfileCust with direct parameters")
                val result = userRepository.editProfileCust(
                    context = context,
                    username = username,
                    name = name,
                    phone = phone,
                    birthDate = birthDate,
                    email = email,
                    imageUri = imageUri
                )

                _editProfileResult.value = result

                // Reload user profile after successful update
                if (result is Result.Success) {
                    Log.d(TAG, "Edit profile successful, reloading profile data")
                    loadUserProfile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in editProfileDirect: ${e.message}")
                e.printStackTrace()
                _editProfileResult.value = Result.Error(e)
            }
        }
    }

    fun logout(){
        viewModelScope.launch {
            try{

            } catch (e: Exception){

            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                // Call the repository to change the password
                val result = userRepository.changePassword(currentPassword, newPassword)

                // Post the result (success or error) to LiveData
                changePasswordResult.postValue(result)
            } catch (e: Exception) {
                // Handle any unexpected errors
                changePasswordResult.postValue(Result.Error(e))
            }
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}