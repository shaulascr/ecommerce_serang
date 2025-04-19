package com.alya.ecommerce_serang.ui.order.address

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.ActivityAddAddressBinding
import com.alya.ecommerce_serang.utils.SavedStateViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import kotlinx.coroutines.launch

class AddAddressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddAddressBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var profileUser: UserProfile
    private lateinit var locationManager: LocationManager

    private var latitude: Double? = null
    private var longitude: Double? = null
    private val provinceAdapter by lazy { ProvinceAdapter(this) }
    private val cityAdapter by lazy { CityAdapter(this) }

    private val viewModel: AddAddressViewModel by viewModels {
        SavedStateViewModelFactory(this) { savedStateHandle ->
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            AddAddressViewModel(orderRepository, savedStateHandle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        setupToolbar()
        setupAutoComplete()
        setupButtonListeners()
        collectFlows()
        requestLocationPermission()


    }

//    private fun viewModelAddAddress(request: CreateAddressRequest) {
//        // Call the private fun in your ViewModel using reflection or expose it in ViewModel
//        val method = AddAddressViewModel::class.java.getDeclaredMethod("addAddress", CreateAddressRequest::class.java)
//        method.isAccessible = true
//        method.invoke(viewModel, request)
//    }
    // UI setup methods
private fun setupToolbar() {
    binding.toolbar.setNavigationOnClickListener {
        onBackPressedDispatcher.onBackPressed()
    }
}

    private fun setupAutoComplete() {
        // Set adapters
        binding.autoCompleteProvinsi.setAdapter(provinceAdapter)
        binding.autoCompleteKabupaten.setAdapter(cityAdapter)

        // Set listeners
        binding.autoCompleteProvinsi.setOnItemClickListener { _, _, position, _ ->
            provinceAdapter.getProvinceId(position)?.let { provinceId ->
                viewModel.getCities(provinceId)
                binding.autoCompleteKabupaten.text.clear()
            }
        }

        binding.autoCompleteKabupaten.setOnItemClickListener { _, _, position, _ ->
            cityAdapter.getCityId(position)?.let { cityId ->
                viewModel.selectedCityId = cityId
            }
        }
    }

    private fun setupButtonListeners() {
        binding.buttonSimpan.setOnClickListener {
            validateAndSubmitForm()
        }
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.provincesState.collect { state ->
                        handleProvinceState(state)
                    }
                }

                launch {
                    viewModel.citiesState.collect { state ->
                        handleCityState(state)
                    }
                }

                launch {
                    viewModel.addressSubmissionState.collect { state ->
                        handleAddressSubmissionState(state)
                    }
                }
            }
        }
    }

    private fun handleProvinceState(state: ViewState<List<ProvincesItem>>) {
        when (state) {
            is ViewState.Loading -> null //showProvinceLoading(true)
            is ViewState.Success -> {
                provinceAdapter.updateData(state.data)
            }
            is ViewState.Error -> {
                showError(state.message)
            }
        }
    }

    private fun handleCityState(state: ViewState<List<CitiesItem>>) {
        when (state) {
            is ViewState.Loading -> null //showCityLoading(true)
            is ViewState.Success -> {
//                showCityLoading(false)
                cityAdapter.updateData(state.data)
            }
            is ViewState.Error -> {
//                showCityLoading(false)
                showError(state.message)
            }
        }
    }

    private fun handleAddressSubmissionState(state: ViewState<String>) {
        when (state) {
            is ViewState.Loading -> showSubmitLoading(true)
            is ViewState.Success -> {
                showSubmitLoading(false)
                showSuccessAndFinish(state.data)
            }
            is ViewState.Error -> {
                showSubmitLoading(false)
                showError(state.message)
            }
        }
    }
    private fun showSubmitLoading(isLoading: Boolean) {
        binding.buttonSimpan.isEnabled = !isLoading
        binding.buttonSimpan.text = if (isLoading) "Menyimpan..." else "Simpan"
        // You might want to show a progress bar as well
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccessAndFinish(message: String) {
        Toast.makeText(this, "Sukses: $message", Toast.LENGTH_SHORT).show()
        onBackPressed()
    }

    private fun validateAndSubmitForm() {
        val lat = latitude
        val long = longitude

        if (lat == null || long == null) {
            showError("Lokasi belum terdeteksi")
            return
        }

        val street = binding.etDetailAlamat.text.toString()
        val subDistrict = binding.etKecamatan.text.toString()
        val postalCode = binding.etKodePos.text.toString()
        val recipient = binding.etNamaPenerima.text.toString()
        val phone = binding.etNomorHp.text.toString()
        val userId = profileUser.userId
        val isStoreLocation = false

        val provinceId = viewModel.selectedProvinceId
        val cityId = viewModel.selectedCityId

        if (street.isBlank() || recipient.isBlank() || phone.isBlank()) {
            showError("Lengkapi semua field wajib")
            return
        }

        if (provinceId == null) {
            showError("Pilih provinsi terlebih dahulu")
            return
        }

        if (cityId == null) {
            showError("Pilih kota/kabupaten terlebih dahulu")
            return
        }

        val request = CreateAddressRequest(
            lat = lat,
            long = long,
            street = street,
            subDistrict = subDistrict,
            cityId = cityId,
            provId = provinceId,
            postCode = postalCode,
            detailAddress = street,
            userId = userId,
            recipient = recipient,
            phone = phone,
            isStoreLocation = isStoreLocation
        )

        viewModel.addAddress(request)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) requestLocation() else Toast.makeText(this, "Izin lokasi ditolak",Toast.LENGTH_SHORT).show()
        }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "Provider lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

        locationManager.requestSingleUpdate(provider, object : LocationListener {
            override fun onLocationChanged(location: Location) {
                latitude = location.latitude
                longitude = location.longitude
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                Toast.makeText(this@AddAddressActivity, "Provider dimatikan", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
