package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
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

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _storeMap = MutableStateFlow<Map<Int, StoreItem>>(emptyMap())
    val storeMap: StateFlow<Map<Int, StoreItem>> = _storeMap.asStateFlow()

    private val _category = MutableLiveData<List<CategoryItem>>()
    val category: LiveData<List<CategoryItem>> get() = _category

    init {
        loadProducts()
        loadCategories()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = productRepository.getAllProducts()) {
                is Result.Success -> _uiState.value = HomeUiState.Success(result.data)
                is Result.Error -> _uiState.value = HomeUiState.Error(result.exception.message ?: "Unknown error")
                is Result.Loading -> {}
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getAllCategories()) {
                is Result.Success -> _categories.value = result.data
                is Result.Error -> Log.e("HomeViewModel", "Failed to fetch categories", result.exception)
                is Result.Loading -> {}
            }
        }
    }

    fun loadStoresForProducts(products: List<ProductsItem>) {
        viewModelScope.launch {
            val map = mutableMapOf<Int, StoreItem>()
            val storeIds = products.mapNotNull { it.storeId }.toSet()

            for (storeId in storeIds) {
                try {
                    val result = productRepository.fetchStoreDetail(storeId)
                    if (result is Result.Success) {
                        map[storeId] = result.data
                    } else if (result is Result.Error) {
                        Log.e("HomeViewModel", "Failed to load storeId $storeId", result.exception)
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Exception fetching storeId $storeId", e)
                }
            }

            _storeMap.value = map
        }
    }

    fun loadCategory() {
        viewModelScope.launch {
            when (val result = productRepository.getAllCategories()) {
                is Result.Success -> _category.value = result.data
                is Result.Error -> Log.e("HomeViewModel", "Failed to fetch categories", result.exception)
                is Result.Loading -> {}
            }
        }
    }


    fun retry() {
        loadProducts()
        loadCategories()
    }


}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val products: List<ProductsItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
