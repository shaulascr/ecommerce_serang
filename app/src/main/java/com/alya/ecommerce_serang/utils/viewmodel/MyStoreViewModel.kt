package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.PaymentUpdate
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.response.store.StoreResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.Payment
import com.alya.ecommerce_serang.data.api.response.store.profile.Shipping
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.NumberFormat
import java.util.Locale

class MyStoreViewModel(private val repository: MyStoreRepository): ViewModel() {
    private var TAG = "MyStoreViewModel"

    private val _myStoreProfile = MutableLiveData<StoreResponse?>()
    val myStoreProfile: LiveData<StoreResponse?> = _myStoreProfile

    private val _storeTypes = MutableLiveData<List<StoreTypesItem>>()
    val storeTypes: LiveData<List<StoreTypesItem>> = _storeTypes

    private val _shipping = MutableLiveData<List<Shipping>>()
    val shipping: LiveData<List<Shipping>> = _shipping

    private val _payment = MutableLiveData<List<Payment>>()
    val payment: LiveData<List<Payment>> = _payment

    private val _isLoadingType = MutableLiveData<Boolean>()
    val isLoadingType: LiveData<Boolean> = _isLoadingType

    private val _updateStoreProfileResult = MutableLiveData<StoreDataResponse>()
    val updateStoreProfileResult: LiveData<StoreDataResponse> = _updateStoreProfileResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage : LiveData<String> = _errorMessage

    private val _balanceResult = MutableLiveData<Result<StoreResponse>>()
    val balanceResult: LiveData<Result<StoreResponse>> get() = _balanceResult

    private val _productList = MutableLiveData<Result<List<ProductsItem>>>()
    val productList: LiveData<Result<List<ProductsItem>>> get() = _productList

    fun loadMyStore(){
        viewModelScope.launch {
            when (val result = repository.fetchMyStoreProfile()){
                is Result.Success -> {
                    val storeData = result.data
                    _myStoreProfile.postValue(storeData)
                    _shipping.postValue(storeData?.shipping)
                    _payment.postValue(storeData?.payment)
                }
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
                    Log.e(TAG, "Store data is null")
                    return@launch
                }

                Log.d("UpdateStoreProfileVM", "Calling repository with params:")
                Log.d("UpdateStoreProfileVM", "storeName: $storeName")
                Log.d("UpdateStoreProfileVM", "description: $description")
                Log.d("UpdateStoreProfileVM", "isOnLeave: $isOnLeave")
                Log.d("UpdateStoreProfileVM", "storeType: $storeType")
                Log.d("UpdateStoreProfileVM", "storeImage: ${storeImage?.headers}")

                val response = repository.updateStoreProfile(
                    storeName = storeName,
                    storeDescription = description,
                    isOnLeave = isOnLeave,
                    storeType = storeType,
                    storeimg = storeImage
                )

                if (response != null) {
                    if (response.isSuccessful) {
                        _updateStoreProfileResult.postValue(response.body())
                        Log.d(TAG, "Update successful: ${response.body()}")
                    } else {
                        _errorMessage.postValue("Gagal memperbarui profil")
                        Log.e(TAG, "Update failed: ${response.errorBody()?.string()}")
                    }
                } else {
                    _errorMessage.postValue("Terjadi kesalahan jaringan atau server")
                    Log.e(TAG, "Repository returned null response")
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Unexpected error")
                Log.e(TAG, "Exception updating store profile", e)
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
                    Log.e(TAG, "Error getting orders count: ${result.exception.message}")
                    0
                }
                is Result.Loading -> 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting orders count", e)
            0
        }
    }

    //count the order
    suspend fun getAllStatusCounts(): Map<String, Int> {
        val statuses = listOf( "unpaid", "paid", "processed")
        val counts = mutableMapOf<String, Int>()

        statuses.forEach { status ->
            counts[status] = getTotalOrdersByStatus(status)
            Log.d(TAG, "Status: $status, countOrder=${counts[status]}")
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

    fun loadMyStoreProducts() {
        viewModelScope.launch {
            _productList.value = Result.Loading
            try {
                val result = repository.fetchMyStoreProducts()
                _productList.value = Result.Success(result)
            } catch (e: Exception) {
                _productList.value = Result.Error(e)
            }
        }
    }

    private fun String.toPlain(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    fun updateStoreApproval(
        storeName: RequestBody,
        description: RequestBody,
        storeType: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        storeProvince: RequestBody,
        storeCity: RequestBody,
        storeSubdistrict: RequestBody,
        storeVillage: RequestBody,
        storeStreet: RequestBody,
        storePostalCode: RequestBody,
        storeAddressDetail: RequestBody,
        userPhone: RequestBody,
        paymentsToUpdate: List<PaymentUpdate> = emptyList(),
        paymentIdToDelete: List<Int> = emptyList(),
        storeCourier: List<String>? = null,
        storeImage: MultipartBody.Part?,
        ktpImage: MultipartBody.Part?,
        npwpDocument: MultipartBody.Part?,
        nibDocument: MultipartBody.Part?
    ) {
        viewModelScope.launch {
            try {
                val store = myStoreProfile.value

                if (store == null) {
                    _errorMessage.postValue("Data toko tidak tersedia")
                    Log.e(TAG, "Store data is null")
                    return@launch
                }

                val response = repository.updateStoreApproval(
                    storeName = storeName,
                    description = description,
                    storeType = storeType,
                    latitude = latitude,
                    longitude = longitude,
                    storeProvince = storeProvince,
                    storeCity = storeCity,
                    storeSubdistrict = storeSubdistrict,
                    storeVillage = storeVillage,
                    storeStreet = storeStreet,
                    storePostalCode = storePostalCode,
                    storeAddressDetail = storeAddressDetail,
                    userPhone = userPhone,
                    paymentsToUpdate = paymentsToUpdate,
                    paymentIdToDelete = paymentIdToDelete,
                    storeCourier = storeCourier,
                    storeImage = storeImage,
                    ktpImage = ktpImage,
                    npwpDocument = npwpDocument,
                    nibDocument = nibDocument
                )

                if (response != null) {
                    if (response.isSuccessful) {
                        _updateStoreProfileResult.postValue(response.body())
                        Log.d(TAG, "Update successful: ${response.body()}")
                    } else {
                        _errorMessage.postValue("Gagal memperbarui profil")
                        Log.e(TAG, "Update failed: ${response.errorBody()?.string()}")
                    }
                } else {
                    _errorMessage.postValue("Terjadi kesalahan jaringan atau server")
                    Log.e(TAG, "Repository returned null response")
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Unexpected error")
                Log.e(TAG, "Exception updating store profile", e)
            }
        }
    }
}