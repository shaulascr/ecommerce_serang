package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.response.store.StoreResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.text.NumberFormat
import java.util.Locale

class MyStoreViewModel(private val repository: MyStoreRepository): ViewModel() {
    private val _myStoreProfile = MutableLiveData<Store?>()
    val myStoreProfile: LiveData<Store?> = _myStoreProfile

    private val _storeTypes = MutableLiveData<List<StoreTypesItem>>()
    val storeTypes: LiveData<List<StoreTypesItem>> = _storeTypes

    private val _isLoadingType = MutableLiveData<Boolean>()
    val isLoadingType: LiveData<Boolean> = _isLoadingType

    private val _updateStoreProfileResult = MutableLiveData<StoreDataResponse>()
    val updateStoreProfileResult: LiveData<StoreDataResponse> = _updateStoreProfileResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage : LiveData<String> = _errorMessage

    private val _balanceResult = MutableLiveData<Result<StoreResponse>>()
    val balanceResult: LiveData<Result<StoreResponse>> get() = _balanceResult

    fun loadMyStore(){
        viewModelScope.launch {
            when (val result = repository.fetchMyStoreProfile()){
                is Result.Success -> _myStoreProfile.postValue(result.data)
                is Result.Error -> _errorMessage.postValue(result.exception.message ?: "Unknown Error")
                is Result.Loading -> null
            }
        }
    }

    fun fetchStoreTypes() {
        _isLoadingType.value = true
        viewModelScope.launch {
            when (val result = repository.listStoreType()) {
                is Result.Success -> {
                    _storeTypes.value = result.data.storeTypes
                    _isLoadingType.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message ?: "Unknown error occurred"
                    _isLoadingType.value = false
                }
                is Result.Loading -> {
                    _isLoadingType.value = true
                }
            }
        }
    }

    fun updateStoreProfile(
        storeName: RequestBody,
        storeType: RequestBody,
        description: RequestBody,
        isOnLeave: RequestBody,
        storeImage: MultipartBody.Part?
    ) {
        viewModelScope.launch {
            try {
                val store = myStoreProfile.value

                if (store == null) {
                    _errorMessage.postValue("Data toko tidak tersedia")
                    return@launch
                }

                val response = repository.updateStoreProfile(
                    storeName = storeName,
                    storeStatus = "active".toRequestBody(),
                    storeDescription = description,
                    isOnLeave = isOnLeave,
                    cityId = store.cityId.toString().toRequestBody(),
                    provinceId = store.provinceId.toString().toRequestBody(),
                    street = store.street.toRequestBody(),
                    subdistrict = store.subdistrict.toRequestBody(),
                    detail = store.detail.toRequestBody(),
                    postalCode = store.postalCode.toRequestBody(),
                    latitude = store.latitude.toRequestBody(),
                    longitude = store.longitude.toRequestBody(),
                    userPhone = store.phone.toRequestBody(),
                    storeType = storeType,
                    storeimg = storeImage
                )
                if (response.isSuccessful) _updateStoreProfileResult.postValue(response.body())
                else _errorMessage.postValue("Gagal memperbarui profil")
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Unexpected error")
            }
        }
    }

    suspend fun getTotalOrdersByStatus(status: String): Int {
        return try {
            when (val result = repository.getSellList(status)) {
                is Result.Success -> {
                    // Access the orders list from the response
                    result.data.orders.size ?: 0
                }
                is Result.Error -> {
                    Log.e("SellsViewModel", "Error getting orders count: ${result.exception.message}")
                    0
                }
                is Result.Loading -> 0
            }
        } catch (e: Exception) {
            Log.e("SellsViewModel", "Exception getting orders count", e)
            0
        }
    }

    //count the order
    suspend fun getAllStatusCounts(): Map<String, Int> {
        val statuses = listOf( "unpaid", "paid", "processed")
        val counts = mutableMapOf<String, Int>()

        statuses.forEach { status ->
            counts[status] = getTotalOrdersByStatus(status)
            Log.d("SellsViewModel", "Status: $status, countOrder=${counts[status]}")
        }

        return counts
    }

    val formattedBalance: LiveData<String> = balanceResult.map { result ->
        when (result) {
            is Result.Success -> {
                val raw = result.data.store.balance.toDouble()
                NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(raw)
            }
            else -> ""
        }
    }

    /** Trigger the network call */
    fun fetchBalance() {
        viewModelScope.launch {
            _balanceResult.value = Result.Loading
            _balanceResult.value = repository.getBalance()
        }
    }

    private fun String.toRequestBody(): RequestBody =
        RequestBody.create("text/plain".toMediaTypeOrNull(), this)
}