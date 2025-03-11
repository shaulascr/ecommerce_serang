package com.alya.ecommerce_serang.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.ProductsItem
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel (
    private val productRepository: ProductRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            when (val result = productRepository.getAllProducts()) {
                is Result.Success -> {
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = HomeUiState.Error(result.exception.message ?: "Unknown error")
                    Log.e("HomeViewModel", "Failed to fetch products", result.exception)
                }
                is Result.Loading-> {

                }
            }
        }
    }


    fun retry() {
        loadProducts()
    }

//    private fun fetchUserData() {
//        viewModelScope.launch {
//            try {
//                val response = apiService.getProtectedData() // Example API request
//                if (response.isSuccessful) {
//                    val data = response.body()
//                    Log.d("HomeFragment", "User Data: $data")
//                    // Update UI with data
//                } else {
//                    Log.e("HomeFragment", "Error: ${response.message()}")
//                }
//            } catch (e: Exception) {
//                Log.e("HomeFragment", "Exception: ${e.message}")
//            }
//        }
//    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val products: List<ProductsItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}