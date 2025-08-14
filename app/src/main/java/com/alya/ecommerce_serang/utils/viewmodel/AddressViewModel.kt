package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.City
import com.alya.ecommerce_serang.data.api.dto.Province
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressesItem
import com.alya.ecommerce_serang.data.repository.AddressRepository
import kotlinx.coroutines.launch

class AddressViewModel(private val addressRepository: AddressRepository) : ViewModel() {

    private val TAG = "AddressViewModel"

    private val _provinces = MutableLiveData<List<Province>>()
    val provinces: LiveData<List<Province>> = _provinces

    private val _cities = MutableLiveData<List<City>>()
    val cities: LiveData<List<City>> = _cities

    private val _storeAddress = MutableLiveData<AddressesItem?>()
    val storeAddress: LiveData<AddressesItem?> get() = _storeAddress

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> get() = _saveSuccess


    fun fetchProvinces() {
        viewModelScope.launch {
            try {
                val response = addressRepository.getProvinces()
                if (response.isSuccessful) {
                    _provinces.value = response.body()?.data ?: emptyList()
                } else {
                    Log.e("EditAddressVM", "Failed to get provinces: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EditAddressVM", "Error getting provinces: ${e.message}")
            }
        }
    }

    fun fetchCities(provinceId: String) {
        viewModelScope.launch {
            try {
                val response = addressRepository.getCities(provinceId)
                if (response.isSuccessful) {
                    _cities.value = response.body()?.cities ?: emptyList()
                } else {
                    Log.e("EditAddressVM", "Failed to get cities: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EditAddressVM", "Error getting cities: ${e.message}")
            }
        }
    }

//    fun fetchProvinces() {
//        Log.d(TAG, "fetchProvinces() called")
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "Calling addressRepository.getProvinces()")
//                val response = addressRepository.getProvinces()
//                Log.d(TAG, "Received provinces response: ${response.size} provinces")
//                _provinces.value = response
//                _isLoading.value = false
//            } catch (e: Exception) {
//                Log.e(TAG, "Error fetching provinces", e)
//                _errorMessage.value = "Failed to load provinces: ${e.message}"
//                _isLoading.value = false
//            }
//        }
//    }

//    fun fetchCities(provinceId: String) {
//        Log.d(TAG, "fetchCities() called with provinceId: $provinceId")
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                selecte
//                Log.d(TAG, "Calling addressRepository.getCities()")
//                val response = addressRepository.getCities(provinceId)
//                Log.d(TAG, "Received cities response: ${response.size} cities")
//                _cities.value = response
//                _isLoading.value = false
//            } catch (e: Exception) {
//                Log.e(TAG, "Error fetching cities", e)
//                _errorMessage.value = "Failed to load cities: ${e.message}"
//                _isLoading.value = false
//            }
//        }
//    }

//    fun fetchStoreAddress() {
//        Log.d(TAG, "fetchStoreAddress() called")
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "Calling addressRepository.getStoreAddress()")
//                val response = addressRepository.getStoreAddress()
//                Log.d(TAG, "Received store address response: $response")
//                _storeAddress.value = response
//                _isLoading.value = false
//            } catch (e: Exception) {
//                Log.e(TAG, "Error fetching store address", e)
//                _errorMessage.value = "Failed to load store address: ${e.message}"
//                _isLoading.value = false
//            }
//        }
//    }

//    fun fetchStoreAddress() {
//        viewModelScope.launch {
//            try {
//                val response = addressRepository.getStoreAddress()
//                if (response.isSuccessful) {
//                    val storeAddress = response.body()?.addresses
//                        ?.firstOrNull { it.isStoreLocation == true }
//
//                    if (storeAddress != null) {
//                        _storeAddress.value = storeAddress
//                    } else {
//                        Log.d("EditAddressVM", "No store address found")
//                    }
//                } else {
//                    Log.e("EditAddressVM", "Failed to get addresses: ${response.message()}")
//                }
//            } catch (e: Exception) {
//                Log.e("EditAddressVM", "Error: ${e.message}")
//            }
//        }
//    }

    fun fetchStoreAddress() {
        viewModelScope.launch {
            try {
                val response = addressRepository.getStoreAddress()
                if (response.isSuccessful) {
                    val storeAddress = response.body()?.addresses
                        ?.firstOrNull { it.isStoreLocation == true }

                    if (storeAddress != null) {
                        _storeAddress.value = storeAddress
                    } else {
                        Log.d(TAG, "No store address found")
                    }
                } else {
                    Log.e(TAG, "Failed to get addresses: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
            }
        }
    }

//    fun saveStoreAddress(
//        provinceId: String,
//        provinceName: String,
//        cityId: String,
//        cityName: String,
//        street: String,
//        subdistrict: String,
//        detail: String,
//        postalCode: String,
//        latitude: Double,
//        longitude: Double
//    ) {
//        Log.d(TAG, "saveStoreAddress() called with provinceId: $provinceId, cityId: $cityId")
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "Calling addressRepository.saveStoreAddress()")
//                val success = addressRepository.saveStoreAddress(
//                    provinceId = provinceId,
//                    provinceName = provinceName,
//                    cityId = cityId,
//                    cityName = cityName,
//                    street = street,
//                    subdistrict = subdistrict,
//                    detail = detail,
//                    postalCode = postalCode,
//                    latitude = latitude,
//                    longitude = longitude
//                )
//                Log.d(TAG, "Save store address result: $success")
//                _saveSuccess.value = success
//                _isLoading.value = false
//            } catch (e: Exception) {
//                Log.e(TAG, "Error saving store address", e)
//                _errorMessage.value = "Failed to save address: ${e.message}"
//                _isLoading.value = false
//            }
//        }
//    }

    fun saveStoreAddress(oldAddress: AddressesItem, newAddress: AddressesItem) {
        val params = buildUpdateBody(oldAddress, newAddress)
        if (params.isEmpty()) {
            Log.d(TAG, "No changes detected")
            _saveSuccess.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = addressRepository.updateAddress(oldAddress.id, params)
                _saveSuccess.value = response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                _saveSuccess.value = false
            }
        }
    }

    private fun buildUpdateBody(oldAddress: AddressesItem, newAddress: AddressesItem): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        fun addIfChanged(key: String, oldValue: Any?, newValue: Any?) {
            if (newValue != null && newValue != oldValue) {
                params[key] = newValue
            }
        }

        addIfChanged("street", oldAddress.street, newAddress.street)
        addIfChanged("province_id", oldAddress.provinceId, newAddress.provinceId)
        addIfChanged("detail", oldAddress.detail, newAddress.detail)
        addIfChanged("subdistrict", oldAddress.subdistrict, newAddress.subdistrict)
        addIfChanged("city_id", oldAddress.cityId, newAddress.cityId)
        addIfChanged("village_id", oldAddress.villageId, newAddress.villageId)
        addIfChanged("postal_code", oldAddress.postalCode, newAddress.postalCode)
        addIfChanged("phone", oldAddress.phone, newAddress.phone)
        addIfChanged("recipient", oldAddress.recipient, newAddress.recipient)
        addIfChanged("latitude", oldAddress.latitude, newAddress.latitude)
        addIfChanged("longitude", oldAddress.longitude, newAddress.longitude)
        addIfChanged("is_store_location", oldAddress.isStoreLocation, newAddress.isStoreLocation)

        return params
    }
}