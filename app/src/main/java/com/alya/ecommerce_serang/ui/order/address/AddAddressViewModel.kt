package com.alya.ecommerce_serang.ui.order.address

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.SubdistrictsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.VillagesItem
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressDetail
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import kotlinx.coroutines.launch

class AddAddressViewModel(private val repository: OrderRepository, private val userRepo: UserRepository, private val savedStateHandle: SavedStateHandle): ViewModel() {
    private val _addressSubmissionState = MutableLiveData<ViewState<String>>()
    val addressSubmissionState: LiveData<ViewState<String>> = _addressSubmissionState

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _errorMessageUser = MutableLiveData<String>()
    val errorMessageUser : LiveData<String> = _errorMessageUser

    private val _provincesState = MutableLiveData<ViewState<List<ProvincesItem>>>()
    val provincesState: LiveData<ViewState<List<ProvincesItem>>> = _provincesState

    private val _citiesState = MutableLiveData<ViewState<List<CitiesItem>>>()
    val citiesState: LiveData<ViewState<List<CitiesItem>>> = _citiesState

    private val _subdistrictState = MutableLiveData<Result<List<SubdistrictsItem>>>()
    val subdistrictState: LiveData<Result<List<SubdistrictsItem>>> = _subdistrictState

    private val _villagesState = MutableLiveData<Result<List<VillagesItem>>>()
    val villagesState: LiveData<Result<List<VillagesItem>>> = _villagesState

    private val _userAddress = MutableLiveData<AddressDetail>()
    val userAddress: LiveData<AddressDetail> = _userAddress

    private val _editAddress = MutableLiveData<Boolean>()
    val editAddress: LiveData<Boolean> get() = _editAddress

    // Stored in SavedStateHandle for configuration changes
    var selectedProvinceId: Int?
        get() = savedStateHandle.get<Int>("selectedProvinceId")
        set(value) { savedStateHandle["selectedProvinceId"] = value }

    var selectedCityId: String?
        get() = savedStateHandle.get<String>("selectedCityId")
        set(value) { savedStateHandle["selectedCityId"] = value }

    var selectedSubdistrict: String? = null
    var selectedSubdistrictId: String? = null
    var selectedVillages: String? = null

    init {
        // Load provinces on initialization
        getProvinces()
    }

    fun addAddress(request: CreateAddressRequest) {
        Log.d(TAG, "Starting address submission process")
        _addressSubmissionState.value = ViewState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling repository.addAddress with request: $request")
                val result = repository.addAddress(request)

                when (result) {
                    is Result.Success -> {
                        val message = result.data.message
                        Log.d(TAG, "Address added successfully: $message")
                        _addressSubmissionState.postValue(ViewState.Success(message))
                    }
                    is Result.Error -> {
                        val errorMsg = result.exception.message ?: "Unknown error"
                        Log.e(TAG, "Error from repository: $errorMsg", result.exception)
                        _addressSubmissionState.postValue(ViewState.Error(errorMsg))
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Repository returned Loading state")
                        // We already set Loading at the beginning
                    }
                    else -> {
                        Log.e(TAG, "Repository returned unexpected result type: $result")
                        _addressSubmissionState.postValue(ViewState.Error("Unexpected error occurred"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred during address submission", e)
                val errorMessage = e.message ?: "Unknown error occurred"
                Log.e(TAG, "Error message: $errorMessage")

                // Log the exception stack trace
                e.printStackTrace()

                _addressSubmissionState.postValue(ViewState.Error(errorMessage))
            }
        }
    }

    fun getProvinces() {
        _provincesState.value = ViewState.Loading
        viewModelScope.launch {
            try {
                val result = repository.getListProvinces()
                if (result?.provinces != null) {
                    _provincesState.postValue(ViewState.Success(result.provinces))
                    Log.d(TAG, "Provinces loaded: ${result.provinces.size}")
                } else {
                    _provincesState.postValue(ViewState.Error("Failed to load provinces"))
                    Log.e(TAG, "Province result was null or empty")
                }
            } catch (e: Exception) {
                _provincesState.postValue(ViewState.Error(e.message ?: "Error loading provinces"))
                Log.e(TAG, "Error fetching provinces", e)
            }
        }
    }

    fun getCities(provinceId: Int){
        _citiesState.value = ViewState.Loading
        viewModelScope.launch {
            try {
                selectedProvinceId = provinceId
                val result = repository.getListCities(provinceId)
                result?.let {
                    _citiesState.postValue(ViewState.Success(it.cities))
                    Log.d(TAG, "Cities loaded for province $provinceId: ${it.cities.size}")
                } ?: run {
                    _citiesState.postValue(ViewState.Error("Failed to load cities"))
                    Log.e(TAG, "City result was null for province $provinceId")
                }
            } catch (e: Exception) {
                _citiesState.postValue(ViewState.Error(e.message ?: "Error loading cities"))
                Log.e(TAG, "Error fetching cities for province $provinceId", e)
            }
        }
    }

    fun getSubdistrict(cityId: String) {
        _subdistrictState.value = Result.Loading
        viewModelScope.launch {
            try {

                selectedSubdistrictId = cityId
                val result = repository.getListSubdistrict(cityId)
                result?.let {
                    _subdistrictState.postValue(Result.Success(it.subdistricts))
                    Log.d(TAG, "Subdistrict loaded for city $cityId: ${it.subdistricts.size}")
                } ?: run {
                    _subdistrictState.postValue(Result.Error(Exception("Failed to load cities")))
                    Log.e(TAG, "Subdistrict result was null for city $cityId")
                }
            } catch (e: Exception) {
                _subdistrictState.postValue(Result.Error(Exception(e.message ?: "Error loading cities")))
                Log.e(TAG, "Error fetching subdistrict for city $cityId", e)
            }
        }
    }

    fun getVillages(subdistrictId: String) {
        _villagesState.value = Result.Loading
        viewModelScope.launch {
            try {

                selectedVillages = subdistrictId
                val result = repository.getListVillages(subdistrictId)
                result?.let {
                    _villagesState.postValue(Result.Success(it.villages))
                    Log.d(TAG, "Villages loaded for subdistrict $subdistrictId: ${it.villages.size}")
                } ?: run {
                    _villagesState.postValue(Result.Error(Exception("Failed to load cities")))
                    Log.e(TAG, "Village result was null for subdistrict $subdistrictId")
                }
            } catch (e: Exception) {
                _villagesState.postValue(Result.Error(Exception(e.message ?: "Error loading cities")))
                Log.e(TAG, "Error fetching villages for subdistrict $subdistrictId", e)
            }
        }
    }

    fun setSelectedProvinceId(id: Int) {
        selectedProvinceId = id
    }

    fun detailAddress(addressId: Int){
        viewModelScope.launch {
            try {
                val response = repository.getAddressDetail(addressId)
                if (response != null){
                    _userAddress.value = response.address
                } else {
                    Log.e(TAG, "Failed load address detail")
                }

            } catch (e:Exception){
                Log.e(TAG, "Error fetching address detail: $e", e)
            }
        }
    }

    fun updateAddress(oldAddress: AddressDetail, newAddress: AddressDetail) {
        val params = buildUpdateBody(oldAddress, newAddress)
        if (params.isEmpty()) {
            Log.d(TAG, "No changes detected")
            _editAddress.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = repository.updateAddress(oldAddress.id, params)
                _editAddress.value = response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                _editAddress.value = false
            }
        }
    }

    private fun buildUpdateBody(oldAddress: AddressDetail, newAddress: AddressDetail): Map<String, Any> {

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
        addIfChanged("village_name", oldAddress.villageName, newAddress.villageName)
        addIfChanged("subdsitrict_id", oldAddress.subdistrictId, newAddress.subdistrictId)
        addIfChanged("id", oldAddress.id, newAddress.id)
        addIfChanged("user_id", oldAddress.userId, newAddress.userId)
        addIfChanged("city_name", oldAddress.cityName, newAddress.cityName)
        addIfChanged("province_name", oldAddress.provinceName, newAddress.provinceName)

        return params
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = repository.fetchUserProfile()) {
                is Result.Success -> {
                    result.data?.let {
                        _userProfile.postValue(it)   // send UserProfile to LiveData
                    } ?: _errorMessageUser.postValue("User data not found")
                }
                is Result.Error -> {
                    _errorMessageUser.postValue(result.exception.message ?: "Unknown error")
                }
                is Result.Loading -> {
                    null
                }
            }
        }
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