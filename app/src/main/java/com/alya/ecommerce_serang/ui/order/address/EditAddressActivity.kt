package com.alya.ecommerce_serang.ui.order.address

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.SubdistrictsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.VillagesItem
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressDetail
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityEditAddressBinding
import com.alya.ecommerce_serang.utils.SavedStateViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class EditAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAddressBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private var latitude: Double? = null
    private var longitude: Double? = null
    private var addressId: Int = -1
    private var currentAddress: AddressDetail? = null
    private val provinceAdapter by lazy { ProvinceAdapter(this) }
    private val cityAdapter by lazy { CityAdapter(this) }
    private val subdistrictAdapter by lazy { SubdsitrictAdapter(this)}
    private val villageAdapter by lazy { VillagesAdapter(this)}

    private var provincesList = mutableListOf<ProvincesItem>()
    private var citiesList = mutableListOf<CitiesItem>()
    private var subdistrictsList = mutableListOf<SubdistrictsItem>()
    private var villagesList = mutableListOf<VillagesItem>()

    private val viewModel: AddAddressViewModel by viewModels {
        SavedStateViewModelFactory(this) { savedStateHandle ->
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            val userRepository = UserRepository(apiService)
            AddAddressViewModel(orderRepository, userRepository, savedStateHandle)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        addressId = intent.getIntExtra(EXTRA_ADDRESS_ID, -1)
        if (addressId == -1) {
            Toast.makeText(this, "Gagal mendapatkan alamat pengguna", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupAdapters()
        setupObservers()
        setupListeners()

        // Load address detail first
        Log.d(TAG, "Loading address with ID: $addressId")
        viewModel.detailAddress(addressId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupAdapters() {
        // Set custom adapters to AutoCompleteTextViews
        binding.autoCompleteProvinsi.setAdapter(provinceAdapter)
        binding.autoCompleteKabupaten.setAdapter(cityAdapter)
        binding.autoCompleteKecamatan.setAdapter(subdistrictAdapter)
        binding.autoCompleteDesa.setAdapter(villageAdapter)
    }

    private fun setupObservers() {
        // Observe address detail
        viewModel.userAddress.observe(this) { address ->
            currentAddress = address
            Log.d(TAG, "Address loaded: $address")
            populateAddressData(address)
        }

        // Observe provinces
        viewModel.provincesState.observe(this) { viewState ->
            when (viewState) {
                is ViewState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ViewState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    provincesList.clear()
                    provincesList.addAll(viewState.data)
                    provinceAdapter.updateData(viewState.data)

                    // Set selected province if address is loaded
                    currentAddress?.let { address ->
                        setSelectedProvince(address.provinceId.toInt())
                    }
                }
                is ViewState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to load province ${viewState.message}")
                }
            }
        }

        // Observe cities
        viewModel.citiesState.observe(this) { viewState ->
            when (viewState) {
                is ViewState.Loading -> {
                    binding.cityProgressBar.visibility = View.VISIBLE
                }
                is ViewState.Success -> {
                    binding.cityProgressBar.visibility = View.GONE
                    citiesList.clear()
                    citiesList.addAll(viewState.data)
                    cityAdapter.updateData(viewState.data)

                    // Set selected city if address is loaded
                    currentAddress?.let { address ->
                        setSelectedCity(address.cityId)
                    }
                }
                is ViewState.Error -> {
                    binding.cityProgressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to load cities ${viewState.message}")
                }
            }
        }

        // Observe subdistricts
        viewModel.subdistrictState.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    // You can add loading indicator for subdistrict if needed
                }
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    subdistrictsList.clear()
                    subdistrictsList.addAll(result.data)
                    subdistrictAdapter.updateData(result.data)

                    // Set selected subdistrict if address is loaded
                    currentAddress?.let { address ->
                        setSelectedSubdistrict(address.subdistrict)
                    }
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    Log.e(TAG, "Failed to load subdistricy")
                }
            }
        }

        // Observe villages
        viewModel.villagesState.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    // You can add loading indicator for village if needed
                }
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    villagesList.clear()
                    villagesList.addAll(result.data)
                    villageAdapter.updateData(result.data)

                    // Set selected village if address is loaded
                    currentAddress?.let { address ->
                        setSelectedVillage(address.villageId)
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to load villages")
                }
            }
        }

        // Observe update result
        viewModel.editAddress.observe(this) { isSuccess ->
            binding.submitProgressBar.visibility = View.GONE
            binding.buttonSimpan.isEnabled = true

            if (isSuccess) {
                Log.d(TAG, "Address updated successfully")
                finish()
            } else {
                Log.d(TAG, "Failed to update address")
            }
        }
    }

    private fun setupListeners() {
        // Province selection
        binding.autoCompleteProvinsi.setOnItemClickListener { _, _, position, _ ->
            val provinceId = provinceAdapter.getProvinceId(position)
            provinceId?.let { id ->
                viewModel.getCities(id)

                // Clear dependent dropdowns
                clearCitySelection()
                clearSubdistrictSelection()
                clearVillageSelection()
            }
        }

        // City selection
        binding.autoCompleteKabupaten.setOnItemClickListener { _, _, position, _ ->
            val cityId = cityAdapter.getCityId(position)
            cityId?.let { id ->
                viewModel.getSubdistrict(id)

                // Clear dependent dropdowns
                clearSubdistrictSelection()
                clearVillageSelection()
            }
        }

        // Subdistrict selection
        binding.autoCompleteKecamatan.setOnItemClickListener { _, _, position, _ ->
            val subdistrictId = subdistrictAdapter.getSubdistrictId(position)
            subdistrictId?.let { id ->
                viewModel.getVillages(id)

                // Clear dependent dropdowns
                clearVillageSelection()
            }
        }

        // Village selection - auto-populate postal code
        binding.autoCompleteDesa.setOnItemClickListener { _, _, position, _ ->
//            val postalCode = villageAdapter.getPostalCode(position)
//            postalCode?.let {
//                binding.etKodePos.setText(it)
//            }
        }

        // Save button
        binding.buttonSimpan.setOnClickListener {
            saveAddress()
        }
    }

    private fun populateAddressData(address: AddressDetail) {
        binding.etNamaPenerima.setText(address.recipient ?: "")
        binding.etNomorHp.setText(address.phone ?: "")
        binding.etDetailAlamat.setText(address.detail ?: "")
        binding.etKodePos.setText(address.postalCode ?: "")

        // Province will be set when provinces are loaded
        // City, subdistrict, village will be set in their respective observers
    }

    private fun setSelectedProvince(provinceId: Int?) {
        provinceId?.let { id ->
            val position = provincesList.indexOfFirst { it.provinceId?.toIntOrNull() == id }
            if (position >= 0) {
                val provinceName = provincesList[position].province
                binding.autoCompleteProvinsi.setText(provinceName, false)
                viewModel.getCities(id)
            }
        }
    }

    private fun setSelectedCity(cityId: String?) {
        cityId?.let { id ->
            val position = citiesList.indexOfFirst { it.cityId?.toString() == id }
            if (position >= 0) {
                val cityName = citiesList[position].cityName
                binding.autoCompleteKabupaten.setText(cityName, false)
                viewModel.getSubdistrict(id)
            }
        }
    }

    private fun setSelectedSubdistrict(subdistrictId: String?) {
        subdistrictId?.let { id ->
            val position = subdistrictsList.indexOfFirst { it.subdistrictId?.toString() == id }
            if (position >= 0) {
                val subdistrictName = subdistrictsList[position].subdistrictName
                binding.autoCompleteKecamatan.setText(subdistrictName, false)
                viewModel.getVillages(id)
            }
        }
    }

    private fun setSelectedVillage(villageId: String?) {
        villageId?.let { id ->
            val position = villagesList.indexOfFirst { it.villageId?.toString() == id }
            if (position >= 0) {
                val villageName = villagesList[position].villageName
                binding.autoCompleteDesa.setText(villageName, false)
            }
        }
    }
//
//    private fun populatePostalCodeFromVillage(villageId: String?) {
//        villageId?.let { id ->
//            val village = villagesList.find { it.villageId?.toString() == id }
//            village?.postalCode?.let { postalCode ->
//                if (binding.etKodePos.text.isNullOrEmpty()) {
//                    binding.etKodePos.setText(postalCode)
//                }
//            }
//        }
//    }

    private fun clearCitySelection() {
        binding.autoCompleteKabupaten.setText("", false)
        citiesList.clear()
        cityAdapter.updateData(emptyList())
    }

    private fun clearSubdistrictSelection() {
        binding.autoCompleteKecamatan.setText("", false)
        subdistrictsList.clear()
        subdistrictAdapter.updateData(emptyList())
    }

    private fun clearVillageSelection() {
        binding.autoCompleteDesa.setText("", false)
        binding.etKodePos.setText("") // Clear postal code when village is cleared
        villagesList.clear()
        villageAdapter.updateData(emptyList())
    }

    private fun saveAddress() {
        currentAddress?.let { oldAddress ->
            val newAddress = createNewAddressFromInputs(oldAddress)

            binding.submitProgressBar.visibility = View.VISIBLE
            binding.buttonSimpan.isEnabled = false

            viewModel.updateAddress(oldAddress, newAddress)
        } ?: run {
            Log.d(TAG, "Address not loaded")
            Toast.makeText(this, "Gagal mendapatkan alamat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewAddressFromInputs(oldAddress: AddressDetail): AddressDetail {
        val selectedProvinceId = getSelectedProvinceId()
        val selectedCityId = getSelectedCityId()
        val selectedSubdistrictId = getSelectedSubdistrictId()
        val selectedVillageId = getSelectedVillageId()

        return oldAddress.copy(
            recipient = binding.etNamaPenerima.text.toString().trim(),
            phone = binding.etNomorHp.text.toString().trim(),
            detail = binding.etDetailAlamat.text.toString().trim(),
            postalCode = binding.etKodePos.text.toString().trim(),
            provinceId = selectedProvinceId.toString(),
            cityId = selectedCityId.toString(),
            subdistrict = selectedSubdistrictId.toString(),
            villageId = selectedVillageId
        )
    }

    private fun getSelectedProvinceId(): Int? {
        val selectedText = binding.autoCompleteProvinsi.text.toString()
        val position = provincesList.indexOfFirst { it.province == selectedText }
        return if (position >= 0) provinceAdapter.getProvinceId(position) else null
    }

    private fun getSelectedCityId(): String? {
        val selectedText = binding.autoCompleteKabupaten.text.toString()
        val position = citiesList.indexOfFirst { it.cityName == selectedText }
        return if (position >= 0) cityAdapter.getCityId(position) else null
    }

    private fun getSelectedSubdistrictId(): String? {
        val selectedText = binding.autoCompleteKecamatan.text.toString()
        val position = subdistrictsList.indexOfFirst { it.subdistrictName == selectedText }
        return if (position >= 0) subdistrictAdapter.getSubdistrictId(position) else null
    }

    private fun getSelectedVillageId(): String? {
        val selectedText = binding.autoCompleteDesa.text.toString()
        val position = villagesList.indexOfFirst { it.villageName == selectedText }
        return if (position >= 0) villageAdapter.getVillageId(position) else null
    }

    companion object {
        const val EXTRA_ADDRESS_ID = "extra_address_id"
        private const val TAG = "EditAddressActivity"
    }

}