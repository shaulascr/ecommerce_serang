package com.alya.ecommerce_serang.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class SearchHomeViewModel (private val productRepository: ProductRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<ProductsItem>>(emptyList())
    val searchResults: LiveData<List<ProductsItem>> = _searchResults

    private val _searchHistory = MutableLiveData<List<String>>(emptyList())
    val searchHistory: LiveData<List<String>> = _searchHistory

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _isSearchActive = MutableLiveData(false)
    val isSearchActive: LiveData<Boolean> = _isSearchActive

    fun searchProducts(query: String) {
        Log.d("HomeViewModel", "searchProducts called with query: '$query'")

        if (query.isBlank()) {
            Log.d("HomeViewModel", "Query is blank, clearing results")
            _searchResults.value = emptyList()
            _isSearchActive.value = false
            return
        }

        _isSearching.value = true
        _isSearchActive.value = true

        viewModelScope.launch {
            Log.d("HomeViewModel", "Starting search coroutine")

            when (val result = productRepository.searchProducts(query)) {
                is Result.Success -> {
                    Log.d("HomeViewModel", "Search successful, found ${result.data.size} products")
                    _searchResults.postValue(result.data)

                    // Double check the state after assignment
                    Log.d("HomeViewModel", "Updated searchResults value has ${result.data.size} items")
                }
                is Result.Error -> {
                    Log.e("HomeViewModel", "Search failed", result.exception)
                    _searchResults.postValue(emptyList())
                }
                else -> {}
            }
            _isSearching.postValue(false)
        }
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
                is Result.Error -> Log.e("HomeViewModel", "Failed to load search history", result.exception)
                else -> {}
            }
        }
    }
}