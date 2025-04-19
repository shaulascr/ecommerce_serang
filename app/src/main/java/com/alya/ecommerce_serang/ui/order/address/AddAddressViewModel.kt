package com.alya.ecommerce_serang.ui.order.address

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddAddressViewModel(private val repository: OrderRepository, private val savedStateHandle: SavedStateHandle): ViewModel() {
    // Flow states for data
    private val _addressSubmissionState = MutableStateFlow<ViewState<String>>(ViewState.Loading)
    val addressSubmissionState = _addressSubmissionState.asStateFlow()

    private val _provincesState = MutableStateFlow<ViewState<List<ProvincesItem>>>(ViewState.Loading)
    val provincesState = _provincesState.asStateFlow()

    private val _citiesState = MutableStateFlow<ViewState<List<CitiesItem>>>(ViewState.Loading)
    val citiesState = _citiesState.asStateFlow()

    // Stored in SavedStateHandle for configuration changes
    var selectedProvinceId: Int?
        get() = savedStateHandle.get<Int>("selectedProvinceId")
        set(value) { savedStateHandle["selectedProvinceId"] = value }

    var selectedCityId: Int?
        get() = savedStateHandle.get<Int>("selectedCityId")
        set(value) { savedStateHandle["selectedCityId"] = value }

    init {
        // Load provinces on initialization
        getProvinces()
    }

    fun addAddress(request: CreateAddressRequest){
        viewModelScope.launch {
            when (val result = repository.addAddress(request)) {
                is Result.Success -> {
                    val message = result.data.message // Ambil `message` dari CreateAddressResponse
                    _addressSubmissionState.value = ViewState.Success(message)
                }
                is Result.Error -> {
                    _addressSubmissionState.value =
                        ViewState.Error(result.exception.message ?: "Unknown error")
                }
                is Result.Loading -> {
                    // Optional, karena sudah set Loading di awal
                }
            }
        }
    }

    fun getProvinces(){
        viewModelScope.launch {
            try {
                val result = repository.getListProvinces()
                result?.let {
                    _provincesState.value = ViewState.Success(it.provinces)
                }
            } catch (e: Exception) {
                Log.e("AddAddressViewModel", "Error fetching provinces: ${e.message}")
            }
        }
    }

    fun getCities(provinceId: Int){
        viewModelScope.launch {
            try {
                selectedProvinceId = provinceId
                val result = repository.getListCities(provinceId)
                result?.let {
                    _citiesState.value = ViewState.Success(it.cities)
                }
            } catch (e: Exception) {
                Log.e("AddAddressViewModel", "Error fetching cities: ${e.message}")
            }
        }
    }

    fun setSelectedProvinceId(id: Int) {
        selectedProvinceId = id
    }

    fun setSelectedCityId(id: Int) {
        selectedCityId = id
    }

    companion object {
        private const val TAG = "AddAddressViewModel"
    }
}

sealed class ViewState<out T> {
    object Loading : ViewState<Nothing>()
    data class Success<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
}