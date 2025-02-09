package com.alya.ecommerce_serang.ui.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.ProductsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel (
    private val productRepository: ProductRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val home = MutableLiveData<AllProductResponse?>(null)
    constructor() : this(ProductRepository(ApiConfig.getApiService()))


    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            productRepository.getAllProducts()
                .onSuccess { products ->
                    _uiState.value = HomeUiState.Success(products)
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    Log.e("ProductViewModel", "Products fetch failed", error)
                }
        }
    }

    fun retry() {
        loadProducts()
    }

//    fun toggleWishlist(product: Product) = viewModelScope.launch {
//        try {
//            productRepository.toggleWishlist(product.id,product.wishlist)
//        }catch (e:Exception){
//
//        }
//    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val products: List<ProductsItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}