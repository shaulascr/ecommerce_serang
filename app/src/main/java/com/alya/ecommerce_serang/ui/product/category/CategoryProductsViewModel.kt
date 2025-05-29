package com.alya.ecommerce_serang.ui.product.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryProductsUiState>(CategoryProductsUiState.Loading)
    val uiState: StateFlow<CategoryProductsUiState> = _uiState.asStateFlow()

    fun loadProductsByCategory(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = CategoryProductsUiState.Loading

            when (val result = productRepository.getProductsByCategory(categoryId)) {
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    _uiState.value = CategoryProductsUiState.Success(result.data)
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    _uiState.value = CategoryProductsUiState.Error(
                        result.exception.message ?: "Failed to load products"
                    )
                }
                is Result.Loading -> {
                    // Handle if needed
                }
            }
        }
    }

    fun retry(categoryId: Int) {
        loadProductsByCategory(categoryId)
    }
}

sealed class CategoryProductsUiState {
    object Loading : CategoryProductsUiState()
    data class Success(val products: List<ProductsItem>) : CategoryProductsUiState()
    data class Error(val message: String) : CategoryProductsUiState()
}