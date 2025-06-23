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
    var selectedCityId: Int? = null

    // Form fields
    val storeName = MutableLiveData<String>()
    val storeDescription = MutableLiveData<String>()
    val storeTypeId = MutableLiveData<Int>()
    val latitude = MutableLiveData<String>()
    val longitude = MutableLiveData<String>()
    val street = MutableLiveData<String>()
    val subdistrict = MutableLiveData<String>()
    val cityId = MutableLiveData<Int>()
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
        val allowedFileTypes = Regex("^(jpeg|jpg|png|pdf)$", RegexOption.IGNORE_CASE)

        // Check each file if present
        if (storeImageUri != null && !ImageUtils.isAllowedFileType(context, storeImageUri, allowedFileTypes)) {
            _errorMessage.value = "Foto toko harus berupa file JPEG, JPG, atau PNG"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (ktpUri != null && !ImageUtils.isAllowedFileType(context, ktpUri, allowedFileTypes)) {
            _errorMessage.value = "KTP harus berupa file JPEG, JPG, atau PNG"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (npwpUri != null && !ImageUtils.isAllowedFileType(context, npwpUri, allowedFileTypes)) {
            _errorMessage.value = "NPWP harus berupa file JPEG, JPG, PNG, atau PDF"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (nibUri != null && !ImageUtils.isAllowedFileType(context, nibUri, allowedFileTypes)) {
            _errorMessage.value = "NIB harus berupa file JPEG, JPG, PNG, atau PDF"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (persetujuanUri != null && !ImageUtils.isAllowedFileType(context, persetujuanUri, allowedFileTypes)) {
            _errorMessage.value = "Persetujuan harus berupa file JPEG, JPG, PNG, atau PDF"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }

        if (qrisUri != null && !ImageUtils.isAllowedFileType(context, qrisUri, allowedFileTypes)) {
            _errorMessage.value = "QRIS harus berupa file JPEG, JPG, PNG, atau PDF"
            _registerState.value = Result.Error(Exception(_errorMessage.value ?: "Invalid file type"))
            return
        }
        viewModelScope.launch {
            try {
                _registerState.value = Result.Loading

                val result = repository.registerStoreUser(
                    context = context,
                    description = storeDescription.value ?: "",
                    storeTypeId = storeTypeId.value ?: 0,
                    latitude = latitude.value ?: "",
                    longitude = longitude.value ?: "",
                    street = street.value ?: "",
                    subdistrict = subdistrict.value ?: "",
                    cityId = cityId.value ?: 0,
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

                _registerState.value = result
            } catch (e: Exception) {
                _registerState.value = Result.Error(e)
            }
        }
    }

//    // Helper function to convert Uri to File
//    private fun getFileFromUri(context: Context, uri: Uri): File {
//        val inputStream = context.contentResolver.openInputStream(uri)
//        val tempFile = File(context.cacheDir, "temp_file_${System.currentTimeMillis()}")
//        inputStream?.use { input ->
//            tempFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//        return tempFile
//    }

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