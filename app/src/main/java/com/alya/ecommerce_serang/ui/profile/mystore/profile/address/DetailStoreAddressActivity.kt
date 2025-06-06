package com.alya.ecommerce_serang.ui.profile.mystore.profile.address

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.BuildConfig
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.City
import com.alya.ecommerce_serang.data.api.dto.Province
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.AddressRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailStoreAddressBinding
import com.alya.ecommerce_serang.utils.viewmodel.AddressViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.snackbar.Snackbar

class DetailStoreAddressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoreAddressBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private var selectedProvinceId: String? = null
    private var selectedCityId: String? = null
    private var provinces: List<Province> = emptyList()
    private var cities: List<City> = emptyList()

    private val TAG = "StoreAddressActivity"

    private val viewModel: AddressViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val addressRepository = AddressRepository(apiService)
            AddressViewModel(addressRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoreAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        // Log the base URL
        Log.d(TAG, "BASE_URL: ${BuildConfig.BASE_URL}")

        // Add error text view
        binding.tvError.visibility = View.GONE

        // Set up header title
        binding.header.headerTitle.text = "Atur Alamat Toko"

        // Set up back button
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupSpinners()
        setupObservers()
        setupSaveButton()
        setupFieldListeners()

        // Add retry button
        binding.btnRetry.setOnClickListener {
            binding.tvError.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            Log.d(TAG, "Retrying to fetch provinces...")
            viewModel.fetchProvinces()
        }

        // Show loading spinners initially
        showProvinceLoading(true)

        // Load existing address data first
        Log.d(TAG, "Fetching store address...")
        viewModel.fetchStoreAddress()

        // Load provinces data
        Log.d(TAG, "Fetching provinces...")
        viewModel.fetchProvinces()
    }

    private fun setupSpinners() {
        // Province spinner listener
        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "Province selected at position: $position")
                if (position > 0 && provinces.isNotEmpty()) {
                    selectedProvinceId = provinces[position - 1].provinceId
                    Log.d(TAG, "Selected province ID: $selectedProvinceId")
                    selectedCityId = null
                    showCityLoading(true)
                    viewModel.fetchCities(selectedProvinceId!!)
                } else {
                    selectedProvinceId = null
                }
                checkAllFieldsFilled()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCityId = if (position > 0) cities[position - 1].cityId else null
                checkAllFieldsFilled()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        // Observe provinces data
        viewModel.provinces.observe(this) { provinceList ->
            Log.d(TAG, "Received provinces: ${provinceList.size}")
            showProvinceLoading(false)

            provinces = provinceList
            val provinceNames = mutableListOf("Pilih Provinsi")
            provinceNames.addAll(provinceList.map { it.provinceName })

            Log.d(TAG, "Province names: $provinceNames")

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                provinceNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerProvince.adapter = adapter

            viewModel.storeAddress.value?.let { address ->
                val index = provinces.indexOfFirst { it.provinceId == address.provinceId }
                if (index != -1) {
                    binding.spinnerProvince.setSelection(index + 1)
                }
            }
        }

        // Observe cities data
        viewModel.cities.observe(this) { cityList ->
            Log.d(TAG, "Received cities: ${cityList.size}")
            showCityLoading(false)

            cities = cityList
            val cityNames = mutableListOf("Pilih Kota/Kabupaten")
            cityNames.addAll(cityList.map { it.cityName })

            Log.d(TAG, "City names: $cityNames")

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                cityNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCity.adapter = adapter

            // If we have a stored city_id, select it
            viewModel.storeAddress.value?.let { address ->
                val index = cities.indexOfFirst { it.cityId == address.cityId }
                if (index != -1) {
                    binding.spinnerCity.setSelection(index + 1)
                }
            }
        }

        // Observe store address data
        viewModel.storeAddress.observe(this) { address ->
            Log.d(TAG, "Received store address: $address")
            address?.let {
                // Set the fields
                binding.edtStreet.setText(it.street)
                binding.edtSubdistrict.setText(it.subdistrict)
                binding.edtDetailAddress.setText(it.detail ?: "")
                binding.edtPostalCode.setText(it.postalCode)
                binding.edtLatitude.setText(it.latitude.toString())
                binding.edtLongitude.setText(it.longitude.toString())
                selectedProvinceId = it.provinceId
                selectedCityId = it.cityId

                // Find province index and select it after provinces are loaded
                if (provinces.isNotEmpty()) {
                    val index = provinces.indexOfFirst { p -> p.provinceId == it.provinceId }
                    Log.d(TAG, "Province index for ID ${address.provinceId}: $index")
                    if (index != -1) {
                        binding.spinnerProvince.setSelection(index + 1) // +1 because of "Pilih Provinsi"
//                        showCityLoading(true)
                        viewModel.fetchCities(it.provinceId)
                    }
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) {
            showError(it)
        }

        // Observe save success
        viewModel.saveSuccess.observe(this) {
            if (it) {
                Toast.makeText(this, "Alamat berhasil disimpan", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveAddress.setOnClickListener {
            val street = binding.edtStreet.text.toString()
            val subdistrict = binding.edtSubdistrict.text.toString()
            val detail = binding.edtDetailAddress.text.toString()
            val postalCode = binding.edtPostalCode.text.toString()
            val latitude = binding.edtLatitude.text.toString().toDoubleOrNull() ?: 0.0
            val longitude = binding.edtLongitude.text.toString().toDoubleOrNull() ?: 0.0

            val city = cities.find { it.cityId == selectedCityId }
            val province = provinces.find { it.provinceId == selectedProvinceId }

            // Validate required fields
            if (selectedProvinceId.isNullOrEmpty() || city == null || street.isEmpty() || subdistrict.isEmpty() || postalCode.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi data yang wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save address
            viewModel.saveStoreAddress(
                provinceId = selectedProvinceId!!,
                provinceName = province?.provinceName ?: "",
                cityId = city.cityId,
                cityName = city.cityName,
                street = street,
                subdistrict = subdistrict,
                detail = detail,
                postalCode = postalCode,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun setupFieldListeners() {
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = checkAllFieldsFilled()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.edtStreet.addTextChangedListener(watcher)
        binding.edtSubdistrict.addTextChangedListener(watcher)
        binding.edtPostalCode.addTextChangedListener(watcher)
    }

    private fun checkAllFieldsFilled() {
        val allValid = !selectedProvinceId.isNullOrEmpty()
                && !selectedCityId.isNullOrEmpty()
                && binding.edtStreet.text.isNotBlank()
                && binding.edtSubdistrict.text.isNotBlank()
                && binding.edtPostalCode.text.isNotBlank()

        binding.btnSaveAddress.let {
            if (allValid) {
                it.isEnabled = true
                it.setBackgroundResource(R.drawable.bg_button_active)
                it.setTextColor(getColor(R.color.white))
            } else {
                it.isEnabled = false
                it.setBackgroundResource(R.drawable.bg_button_disabled)
                it.setTextColor(getColor(R.color.black_300))
            }
        }
    }

    private fun showProvinceLoading(isLoading: Boolean) {
        binding.provinceProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.spinnerProvince.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showCityLoading(isLoading: Boolean) {
        binding.cityProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.spinnerCity.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = "Error: $message\nURL: ${BuildConfig.BASE_URL}/provinces"
        binding.btnRetry.visibility = View.VISIBLE

        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("$message\n\nAPI URL: ${BuildConfig.BASE_URL}/provinces")
            .setPositiveButton("Retry") { _, _ ->
                binding.tvError.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                viewModel.fetchProvinces()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                binding.tvError.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                viewModel.fetchProvinces()
            }
            .show()
    }
}