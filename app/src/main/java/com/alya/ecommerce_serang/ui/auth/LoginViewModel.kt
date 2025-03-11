package com.alya.ecommerce_serang.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.LoginResponse
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserRepository) : ViewModel() {
    private val _loginState = MutableLiveData<com.alya.ecommerce_serang.data.repository.Result<LoginResponse>>()
    val loginState: LiveData<Result<LoginResponse>> get() = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = com.alya.ecommerce_serang.data.repository.Result.Loading
            val result = repository.login(email, password)
            _loginState.value = result
        }
    }
}