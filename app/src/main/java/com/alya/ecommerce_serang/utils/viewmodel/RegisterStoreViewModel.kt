package com.alya.ecommerce_serang.utils.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.auth.RegisterStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.utils.ImageUtils
import kotlinx.coroutines.launch

class RegisterStoreViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // LiveData for UI state
    private val _registerState = MutableLiveData<Result<RegisterStoreResponse>>()
    val registerState: LiveData<Result<RegisterStoreResponse>> = _registerState

    private val _storeTypes = MutableLiveData<List<StoreTypesItem>>()
    val storeTypes: LiveData<List<StoreTypesItem>> = _storeTypes

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // LiveData for loading state
    private val _isLoadingType = MutableLiveData<Boolean>()
    val isLoadingType: LiveData<Boolean> = _isLoadingType

    private val _provincesState = MutableLiveData<Result<List<ProvincesItem>>>()
    val provincesState: LiveData<Result<List<ProvincesItem>>> = _provincesState

    private val _citiesState = MutableLiveData<Result<List<CitiesItem>>>()
    val citiesState: LiveData<Result<List<CitiesItem>>> = _citiesState

    var selectedProvinceId: Int? = null
    var selectedCityId: String? = null

    // Form fields
    val storeName = MutableLiveData<String>()
    val storeDescription = MutableLiveData<String>()
    val storeTypeId = MutableLiveData<Int>()
    val latitude = MutableLiveData<String>()
    val longitude = MutableLiveData<String>()
    val street = MutableLiveData<String>()
    val subdistrict = MutableLiveData<String>()
    val cityId = MutableLiveData<String>()
    val provinceId = MutableLiveData<Int>()
    val postalCode = MutableLiveData<Int>()
    val addressDetail = MutableLiveData<String>()
    val bankName = MutableLiveData<String>()
    val bankNumber = MutableLiveData<Int>()
    val accountName = MutableLiveData<String>()

    // Files
    var storeImageUri: Uri? = null
    var ktpUri: Uri? = null
    var npwpUri: Uri? = null
    var nibUri: Uri? = null
    var persetujuanUri: Uri? = null
    var qrisUri: Uri? = null

    // Selected couriers
    val selectedCouriers = mutableListOf<String>()

    fun registerStore(context: Context) {
        Log.d(TAG, "Starting registerStore()")

        val allowedFileTypes = Regex("^(jpeg|jpg|png|pdf)$", RegexOption.IGNORE_CASE)

        fun logFileInfo(label: String, uri: Uri?) {
            if (uri == null) {
                Log.d(TAG, "$label URI: null")
                return
            }
            Log.d(TAG, "$label URI: $uri")
            try {
                val fileSizeBytes = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize
                } ?: -1
                Log.d(TAG, "$label original size: ${fileSizeBytes / 1024} KB")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting size for $label", e)
            }
        }

        // Log all file info before validation
        logFileInfo("Store Image", storeImageUri)
        logFileInfo("KTP", ktpUri)
        logFileInfo("NPWP", npwpUri)
        logFileInfo("NIB", nibUri)
        logFileInfo("Persetujuan", persetujuanUri)
        logFileInfo("QRIS", qrisUri)

        // Check file types
        if (storeImageUri != null && !ImageUtils.isAllowedFileType(context, storeImageUri, allowedFileTypes)) {
            _errorMessage.value = "Foto toko harus berupa file JPEG, JPG, atau PNG"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for store image")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (ktpUri != null && !ImageUtils.isAllowedFileType(context, ktpUri, allowedFileTypes)) {
            _errorMessage.value = "KTP harus berupa file JPEG, JPG, atau PNG"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for KTP")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (npwpUri != null && !ImageUtils.isAllowedFileType(context, npwpUri, allowedFileTypes)) {
            _errorMessage.value = "NPWP harus berupa file JPEG, JPG, PNG, atau PDF"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for NPWP")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (nibUri != null && !ImageUtils.isAllowedFileType(context, nibUri, allowedFileTypes)) {
            _errorMessage.value = "NIB harus berupa file JPEG, JPG, PNG, atau PDF"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for NIB")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (persetujuanUri != null && !ImageUtils.isAllowedFileType(context, persetujuanUri, allowedFileTypes)) {
            _errorMessage.value = "Persetujuan harus berupa file JPEG, JPG, PNG, atau PDF"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for Persetujuan")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (qrisUri != null && !ImageUtils.isAllowedFileType(context, qrisUri, allowedFileTypes)) {
            _errorMessage.value = "QRIS harus berupa file JPEG, JPG, PNG, atau PDF"
            Log.e(TAG, _errorMessage.value ?: "Invalid file type for QRIS")
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        Log.d(TAG, "File type checks passed. Starting repository.registerStoreUser() call.")

        viewModelScope.launch {
            try {
                _registerState.value = Result.Loading
                Log.d(TAG, "Register store request payload: " +
                        "storeName=${storeName.value}, storeTypeId=${storeTypeId.value}, " +
                        "lat=${latitude.value}, long=${longitude.value}, " +
                        "street=${street.value}, subdistrict=${subdistrict.value}, " +
                        "cityId=${cityId.value}, provinceId=${provinceId.value}, postalCode=${postalCode.value}, " +
                        "bankName=${bankName.value}, bankNum=${bankNumber.value}, accountName=${accountName.value}, " +
                        "selectedCouriers=$selectedCouriers")

                val result = repository.registerStoreUser(
                    context = context,
                    description = storeDescription.value ?: "",
                    storeTypeId = storeTypeId.value ?: 0,
                    latitude = latitude.value ?: "",
                    longitude = longitude.value ?: "",
                    street = street.value ?: "",
                    subdistrict = subdistrict.value ?: "",
                    cityId = cityId.value ?: "",
                    provinceId = provinceId.value ?: 0,
                    postalCode = postalCode.value ?: 0,
                    detail = addressDetail.value ?: "",
                    bankName = bankName.value ?: "",
                    bankNum = bankNumber.value ?: 0,
                    storeName = storeName.value ?: "",
                    storeImg = storeImageUri,
                    ktp = ktpUri,
                    npwp = npwpUri,
                    nib = nibUri,
                    persetujuan = persetujuanUri,
                    couriers = selectedCouriers,
                    qris = qrisUri,
                    accountName = accountName.value ?: ""
                )

                Log.d(TAG, "Repository returned result: $result")
                _registerState.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Exception during registerStore", e)
                _registerState.value = Result.Error(e)
            }
        }
    }

    fun validateForm(): Boolean {
        // Implement form validation logic
        return !(storeName.value.isNullOrEmpty() ||
                storeTypeId.value == null ||
                street.value.isNullOrEmpty() ||
                subdistrict.value.isNullOrEmpty() ||
                cityId.value == null ||
                provinceId.value == null ||
                postalCode.value == null ||
                bankName.value.isNullOrEmpty() ||
                bankNumber.value == null ||
                selectedCouriers.isEmpty() ||
                ktpUri == null ||
                nibUri == null)
    }

    // Function to fetch store types
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

    fun getProvinces() {
        _provincesState.value = Result.Loading
        viewModelScope.launch {
            try {
                val result = repository.getListProvinces()
                if (result?.provinces != null) {
                    _provincesState.postValue(Result.Success(result.provinces))
                    Log.d(TAG, "Provinces loaded: ${result.provinces.size}")
                } else {
                    _provincesState.postValue(Result.Error(Exception("Failed to load provinces")))
                    Log.e(TAG, "Province result was null or empty")
                }
            } catch (e: Exception) {
                _provincesState.postValue(Result.Error(Exception(e.message ?: "Error loading provinces")))
                Log.e(TAG, "Error fetching provinces", e)
            }
        }
    }

    fun getCities(provinceId: Int){
        _citiesState.value = Result.Loading
        viewModelScope.launch {
            try {
                selectedProvinceId = provinceId
                val result = repository.getListCities(provinceId)
                result?.let {
                    _citiesState.postValue(Result.Success(it.cities))
                    Log.d(TAG, "Cities loaded for province $provinceId: ${it.cities.size}")
                } ?: run {
                    _citiesState.postValue(Result.Error(Exception("Failed to load cities")))
                    Log.e(TAG, "City result was null for province $provinceId")
                }
            } catch (e: Exception) {
                _citiesState.postValue(Result.Error(Exception(e.message ?: "Error loading cities")))
                Log.e(TAG, "Error fetching cities for province $provinceId", e)
            }
        }
    }

    companion object {
        private const val TAG = "RegisterStoreUserViewModel"
    }
}