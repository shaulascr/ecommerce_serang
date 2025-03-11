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

            productRepository.getAllProducts().let { result ->
                when (result) {
                    is Result.Success -> {
                        // Handle the success case
                        _uiState.value = HomeUiState.Success(result.data)  // result.data contains the list of products
                    }
                    is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                        // Handle the error case
                        _uiState.value = HomeUiState.Error(result.exception.message ?: "Unknown error")
                        Log.e("HomeViewModel", "Failed to fetch products", result.exception)
                    }
                    com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                        // Optionally handle the loading state if needed
                    }
                }
            }
        }
    }


    fun retry() {
        loadProducts()
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val products: List<ProductsItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}