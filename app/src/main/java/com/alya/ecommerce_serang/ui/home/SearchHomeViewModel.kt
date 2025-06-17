package com.alya.ecommerce_serang.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class SearchHomeViewModel (private val productRepository: ProductRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<ProductsItem>>(emptyList())
    val searchResults: LiveData<List<ProductsItem>> = _searchResults

    private val _storeDetail =  MutableLiveData<Map<Int, StoreItem>>()
    val storeDetail : LiveData<Map<Int, StoreItem>> get() = _storeDetail

    private val _searchHistory = MutableLiveData<List<String>>(emptyList())
    val searchHistory: LiveData<List<String>> = _searchHistory

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _isSearchActive = MutableLiveData(false)
    val isSearchActive: LiveData<Boolean> = _isSearchActive

    fun searchProducts(query: String) {
        Log.d("SearchHomeViewModel", "searchProducts called with query: '$query'")

        if (query.isBlank()) {
            Log.d("SearchHomeViewModel", "Query is blank, clearing results")
            _searchResults.value = emptyList()
            _isSearchActive.value = false
            return
        }

        _isSearching.value = true
        _isSearchActive.value = true

        viewModelScope.launch {
            Log.d("SearchHomeViewModeldel", "Starting search coroutine")

            when (val result = productRepository.searchProducts(query)) {
                is Result.Success -> {
                    Log.d("SearchHomeViewModel", "Search successful, found ${result.data.size} products")
                    _searchResults.postValue(result.data)

                    // Double check the state after assignment
                    Log.d("SearchHomeViewModel", "Updated searchResults value has ${result.data.size} items")
                }
                is Result.Error -> {
                    Log.e("SearchHomeViewModel", "Search failed", result.exception)
                    _searchResults.postValue(emptyList())
                }
                else -> {}
            }
            _isSearching.postValue(false)
        }
    }

    fun storeDetail(products: List<ProductsItem>){
        viewModelScope.launch {
            val map = mutableMapOf<Int, StoreItem>()

            val storeIds = products.mapNotNull { it.storeId }.toSet()
            for (storeId in storeIds){
                try {
                    when (val result = productRepository.fetchStoreDetail(storeId)){
                        is Result.Success -> map[storeId] = result.data
                        is Result.Error -> Log.e("SearchHomeViewModel", "Error Loading Store")
                        else -> {}
                    }
                } catch (e: Exception){
                    Log.e("SearchHomeViewModel", "Exception error for storeId: $storeId", e)
                }
            }
            _storeDetail.value = map
        }
    }

    fun loadStoreDetail(storeId: Int) {

    }

    fun clearSearch() {
        _isSearchActive.value = false
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun loadSearchHistory() {
        viewModelScope.launch {
            when (val result = productRepository.getSearchHistory()) {
                is Result.Success -> _searchHistory.value = result.data
                is Result.Error -> Log.e("SearchHomeViewModel", "Failed to load search history", result.exception)
                else -> {}
            }
        }
    }
}