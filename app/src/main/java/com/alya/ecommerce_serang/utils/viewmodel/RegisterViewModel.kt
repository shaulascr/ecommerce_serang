package com.alya.ecommerce_serang.utils.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.auth.User
import com.alya.ecommerce_serang.data.api.response.auth.VerifRegisterResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.SubdistrictsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.VillagesItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.utils.SessionManager
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: UserRepository, private val orderRepo: OrderRepository, private val context: Context) : ViewModel() {

    private val _loginState = MutableLiveData<Result<LoginResponse>>()
    val loginState: LiveData<Result<LoginResponse>> get() = _loginState

    // To track if user is authenticated
    private val _isAuthenticated = MutableLiveData<Boolean>(false)
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    private var _lastCheckedField = MutableLiveData<String>()
    val lastCheckedField: String
        get() = _lastCheckedField.value ?: ""

    private val _userData = MutableLiveData<RegisterRequest>()
    val userData: LiveData<RegisterRequest> = _userData

    // Current step in the registration process
    private val _currentStep = MutableLiveData<Int>(1)
    val currentStep: LiveData<Int> = _currentStep
    // MutableLiveData for handling register state (Loading, Success, or Error)
    private val _registerState = MutableLiveData<Result<String>>()
    val registerState: LiveData<Result<String>> = _registerState

    // MutableLiveData for handling OTP request state
    private val _otpState = MutableLiveData<Result<Unit>>()
    val otpState: LiveData<Result<Unit>> = _otpState

    private val _checkValue = MutableLiveData<Result<Boolean>>()
    val checkValue: LiveData<Result<Boolean>> = _checkValue

    // MutableLiveData to store messages from API responses
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _registeredUser = MutableLiveData<User>()
    val registeredUser: LiveData<User> = _registeredUser

    private val _toastMessage = MutableLiveData<com.alya.ecommerce_serang.utils.viewmodel.Event<String>>()
    val toastMessage: LiveData<com.alya.ecommerce_serang.utils.viewmodel.Event<String>> = _toastMessage

    // For address data
    var selectedProvinceId: Int? = null
    var selectedCityId: String? = null
    var selectedSubdistrict: String? = null
    var selectedVillages: String? = null
    var selectedPostalCode: String? = null

    // For provinces and cities using raja ongkir
    private val _provincesState = MutableLiveData<ViewState<List<ProvincesItem>>>()
    val provincesState: LiveData<ViewState<List<ProvincesItem>>> = _provincesState

    private val _citiesState = MutableLiveData<ViewState<List<CitiesItem>>>()
    val citiesState: LiveData<ViewState<List<CitiesItem>>> = _citiesState

    private val _subdistrictState = MutableLiveData<ViewState<List<SubdistrictsItem>>>()
    val subdistrictState: LiveData<ViewState<List<SubdistrictsItem>>> = _subdistrictState

    private val _villagesState = MutableLiveData<ViewState<List<VillagesItem>>>()
    val villagesState: LiveData<ViewState<List<VillagesItem>>> = _villagesState

    // For address submission
    private val _addressSubmissionState = MutableLiveData<ViewState<String>>()
    val addressSubmissionState: LiveData<ViewState<String>> = _addressSubmissionState

    private val sessionManager by lazy { SessionManager(context) }

    // For authenticated API calls
    private fun getAuthenticatedApiService(): ApiService {
        return ApiConfig.getApiService(sessionManager)
    }

    fun updateUserData(updatedData: RegisterRequest) {
        _userData.value = updatedData
    }

    // Set current step
    fun setStep(step: Int) {
        _currentStep.value = step
    }
    /**
     * Function to request OTP by sending an email to the API.
     * - It sets the OTP state to `Loading` before calling the repository.
     * - If successful, it updates `_message` with the response message and signals success.
     * - If an error occurs, it updates `_otpState` with `Result.Error` and logs the failure.
     */

    fun requestOtp(email: String) {
        viewModelScope.launch {
            _otpState.value = Result.Loading // Indicating API call in progress

            try {
                // Call the repository function to request OTP
//                val authenticatedApiService = getAuthenticatedApiService()
//                val authenticatedOrderRepo = UserRepository(authenticatedApiService)
                val response: OtpResponse = repository.requestOtpRep(email)

                // Log and store success message
                Log.d("RegisterViewModel", "OTP Response: ${response.message}")
                _message.value = response.message // Store the message for UI feedback

                // Update state to indicate success
                _otpState.value = Result.Success(Unit)

            } catch (exception: Exception) {
                // Handle any errors and update state
                _otpState.value = Result.Error(exception)
                _message.value = exception.localizedMessage ?: "Failed to request OTP"

                // Log the error for debugging
                Log.e("RegisterViewModel", "OTP request failed for: $email", exception)
            }
        }
    }

    /**
     * Function to register a new user.
     * - It first sets `_registerState` to `Loading` to indicate the process is starting.
     * - Calls the repository function to handle user registration.
     * - If successful, it updates `_message` and signals success with the response message.
     * - If an error occurs, it updates `_registerState` with `Result.Error` and logs the failure.
     */
    fun registerUser(request: RegisterRequest) {
        viewModelScope.launch {
            _registerState.value = Result.Loading // Indicating API call in progress

            try {
                // Call repository function to register the user
                val response: RegisterResponse = repository.registerUser(request)

                Log.d(TAG, "Registration API call successful")
                Log.d(TAG, "Response message: ${response.message}")
                Log.d(TAG, "User ID received: ${response.user.id}")
                Log.d(TAG, "User details - Name: ${response.user.name}, Email: ${response.user.email}, Username: ${response.user.username}")

                // Store the user data
                _registeredUser.value = response.user
                Log.d(TAG, "User data stored in ViewModel")

                // Store success message
                _message.value = response.message
                Log.d(TAG, "Success message stored: ${response.message}")

                _registerState.value = Result.Success(response.message)

                // Automatically login after successful registration
                request.email?.let { email ->

                    request.password?.let { password ->
                        Log.d(TAG, "Attempting auto-login with email: $email")

                        login(email, password)
                    }
                }

            } catch (exception: Exception) {
                Log.e(TAG, "Registration failed with exception: ${exception.javaClass.simpleName}", exception)
                Log.e(TAG, "Exception message: ${exception.message}")
                Log.e(TAG, "Exception cause: ${exception.cause}")
                // Handle any errors and update state
                _registerState.value = Result.Error(exception)

                _message.value = exception.localizedMessage ?: "Registration failed"
                Log.d(TAG, "Error message stored: ${exception.localizedMessage ?: "Registration failed"}")


                // Log the error for debugging
                Log.e("RegisterViewModel", "User registration failed", exception)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Result.Loading
            try {
                val result = repository.login(email, password)
                _loginState.value = result

                // Update authentication status if login was successful
                if (result is Result.Success) {
                    _isAuthenticated.value = true
                }
            } catch (exception: Exception) {
                _loginState.value = Result.Error(exception)
                Log.e("RegisterViewModel", "Login failed", exception)
            }
        }
    }

    fun checkValueReg(request: VerifRegisReq){
        _lastCheckedField.value = request.fieldRegis
        viewModelScope.launch {
            try {
                // Call the repository function to request OTP
                val response: VerifRegisterResponse = repository.checkValue(request)

                // Log and store success message
                Log.d("RegisterViewModel", "OTP Response: ${response.available}")
                _checkValue.value = Result.Success(response.available)// Store the message for UI feedback

                val msg = if (response.available)
                    "${request.fieldRegis.capitalize()} dapat digunakan"
                else
                    "${request.fieldRegis.capitalize()} sudah terdaftar"
                _toastMessage.value = Event(msg)

            } catch (exception: Exception) {
                // Handle any errors and update state
                _checkValue.value = Result.Error(exception)
                _toastMessage.value = Event("Gagal memeriksa ${request.fieldRegis}")

                // Log the error for debugging
                Log.e("RegisterViewModel", "Error:", exception)
            }
        }
    }
//using raja ongkir
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

    //kota pake raja ongkir
    fun getCities(provinceId: Int) {
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
        _subdistrictState.value = ViewState.Loading
        viewModelScope.launch {
            try {

                selectedSubdistrict = cityId
                val result = repository.getListSubdistrict(cityId)
                result?.let {
                    _subdistrictState.postValue(ViewState.Success(it.subdistricts))
                    Log.d(TAG, "Cities loaded for province $cityId: ${it.subdistricts.size}")
                } ?: run {
                    _subdistrictState.postValue(ViewState.Error("Failed to load cities"))
                    Log.e(TAG, "City result was null for province $cityId")
                }
            } catch (e: Exception) {
                _subdistrictState.postValue(ViewState.Error(e.message ?: "Error loading cities"))
                Log.e(TAG, "Error fetching cities for province $cityId", e)
            }
        }
    }

    fun getVillages(subdistrictId: String) {
        _villagesState.value = ViewState.Loading
        viewModelScope.launch {
            try {

                selectedVillages = subdistrictId
                val result = repository.getListVillages(subdistrictId)
                result?.let {
                    _villagesState.postValue(ViewState.Success(it.villages))
                    Log.d(TAG, "Cities loaded for province $subdistrictId: ${it.villages.size}")
                } ?: run {
                    _villagesState.postValue(ViewState.Error("Failed to load cities"))
                    Log.e(TAG, "City result was null for province $subdistrictId")
                }
            } catch (e: Exception) {
                _villagesState.postValue(ViewState.Error(e.message ?: "Error loading cities"))
                Log.e(TAG, "Error fetching cities for province $subdistrictId", e)
            }
        }
    }

    fun setSelectedProvinceId(id: Int) {
        selectedProvinceId = id
    }

    fun updateSelectedCityId(id: String) {
        selectedCityId = id
    }

    fun updateSelectedSubdistrict(id: String){
        selectedSubdistrict = id
    }

    fun updateSelectedVillages(id: String){
        selectedVillages = id
    }

    fun addAddress(request: CreateAddressRequest) {
        Log.d(TAG, "Starting address submission process")
        _addressSubmissionState.value = ViewState.Loading
        viewModelScope.launch {
            try {
                val authenticatedApiService = getAuthenticatedApiService()
                val authenticatedOrderRepo = OrderRepository(authenticatedApiService)
                Log.d(TAG, "Calling repository.addAddress with request: $request")
                val result = authenticatedOrderRepo.addAddress(request)

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

    companion object {
        private const val TAG = "RegisterViewModel"
    }

}

class Event<out T>(private val data: T) {
    private var handled = false
    fun getContentIfNotHandled(): T? = if (handled) null else { handled = true; data }
}