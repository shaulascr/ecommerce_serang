package com.alya.ecommerce_serang.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityRegisterStoreBinding
import com.alya.ecommerce_serang.ui.order.address.CityAdapter
import com.alya.ecommerce_serang.ui.order.address.ProvinceAdapter
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class RegisterStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterStoreBinding
    private lateinit var sessionManager: SessionManager

    private lateinit var provinceAdapter: ProvinceAdapter
    private lateinit var cityAdapter: CityAdapter
    // Request codes for file picking
    private val PICK_STORE_IMAGE_REQUEST = 1001
    private val PICK_KTP_REQUEST = 1002
    private val PICK_NPWP_REQUEST = 1003
    private val PICK_NIB_REQUEST = 1004
    private val PICK_PERSETUJUAN_REQUEST = 1005
    private val PICK_QRIS_REQUEST = 1006

    // Location request code
    private val LOCATION_PERMISSION_REQUEST = 2001

    private val viewModel: RegisterStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = UserRepository(apiService)
            RegisterStoreViewModel(orderRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RegisterStoreActivity")
        binding = ActivityRegisterStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        Log.d(TAG, "onCreate: SessionManager initialized")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Log.d(TAG, "onCreate: Window decoration set")

        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled")

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            Log.d(TAG, "onCreate: Applying window insets")
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        provinceAdapter = ProvinceAdapter(this)
        cityAdapter = CityAdapter(this)
        Log.d(TAG, "onCreate: Adapters initialized")

        setupDataBinding()
        Log.d(TAG, "onCreate: Data binding setup completed")

        setupSpinners() // Location spinners
        Log.d(TAG, "onCreate: Spinners setup completed")

        // Setup observers
        setupStoreTypesObserver() // Store type observer
        setupObservers()
        Log.d(TAG, "onCreate: Observers setup completed")

        setupMap()
        Log.d(TAG, "onCreate: Map setup completed")

        setupDocumentUploads()
        Log.d(TAG, "onCreate: Document uploads setup completed")

        setupCourierSelection()
        Log.d(TAG, "onCreate: Courier selection setup completed")

        Log.d(TAG, "onCreate: Fetching store types from API")
        viewModel.fetchStoreTypes()

        Log.d(TAG, "onCreate: Fetching provinces from API")
        viewModel.getProvinces()


        // Setup register button
        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "Register button clicked")
            if (viewModel.validateForm()) {
                Log.d(TAG, "Form validation successful, proceeding with registration")
                viewModel.registerStore(this)
            } else {
                Log.e(TAG, "Form validation failed")
                Toast.makeText(this, "Harap lengkapi semua field yang wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d(TAG, "onCreate: RegisterStoreActivity setup completed")
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up LiveData observers")

        // Observe province state
        viewModel.provincesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "setupObservers: Loading provinces...")
                    binding.provinceProgressBar?.visibility = View.VISIBLE
                    binding.spinnerProvince.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "setupObservers: Provinces loaded successfully: ${state.data.size} provinces")
                    binding.provinceProgressBar?.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true

                    // Update adapter with data
                    provinceAdapter.updateData(state.data)
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Error loading provinces: ${state.exception.message}")
                    binding.provinceProgressBar?.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true
                }
            }
        }

        // Observe city state
        viewModel.citiesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "setupObservers: Loading cities...")
                    binding.cityProgressBar?.visibility = View.VISIBLE
                    binding.spinnerCity.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "setupObservers: Cities loaded successfully: ${state.data.size} cities")
                    binding.cityProgressBar?.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true

                    // Update adapter with data
                    cityAdapter.updateData(state.data)
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Error loading cities: ${state.exception.message}")
                    binding.cityProgressBar?.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true
                }
            }
        }

        // Observe registration state
        viewModel.registerState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    Log.d(TAG, "setupObservers: Store registration in progress...")
                    showLoading(true)
                }
                is Result.Success -> {
                    Log.d(TAG, "setupObservers: Store registration successful")
                    showLoading(false)
                    Toast.makeText(this, "Toko berhasil didaftarkan", Toast.LENGTH_SHORT).show()
                    finish() // Return to previous screen
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Store registration failed: ${result.exception.message}")
                    showLoading(false)
                    Toast.makeText(this, "Gagal mendaftarkan toko: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Log.d(TAG, "setupObservers: Observers setup completed")
    }

    private fun setupStoreTypesObserver() {
        Log.d(TAG, "setupStoreTypesObserver: Setting up store types observer")

        // Observe loading state
        viewModel.isLoadingType.observe(this) { isLoading ->
            if (isLoading) {
                Log.d(TAG, "setupStoreTypesObserver: Loading store types...")
                // Show loading indicator for store types spinner
                binding.spinnerStoreType.isEnabled = false
                binding.storeTypeProgressBar?.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "setupStoreTypesObserver: Store types loading completed")
                binding.spinnerStoreType.isEnabled = true
                binding.storeTypeProgressBar?.visibility = View.GONE
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Log.e(TAG, "setupStoreTypesObserver: Error loading store types: $errorMsg")
                Toast.makeText(this, "Error loading store types: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe store types data
        viewModel.storeTypes.observe(this) { storeTypes ->
            Log.d(TAG, "setupStoreTypesObserver: Store types loaded: ${storeTypes.size}")
            if (storeTypes.isNotEmpty()) {
                // Add "Pilih Jenis UMKM" as the first item if it's not already there
                val displayList = if (storeTypes.any { it.name == "Pilih Jenis UMKM" || it.id == 0 }) {
                    Log.d(TAG, "setupStoreTypesObserver: Default item already exists in store types list")
                    storeTypes
                } else {
                    Log.d(TAG, "setupStoreTypesObserver: Adding default item to store types list")
                    val defaultItem = StoreTypesItem(name = "Pilih Jenis UMKM", id = 0)
                    listOf(defaultItem) + storeTypes
                }

                // Setup spinner with API data
                setupStoreTypeSpinner(displayList)
            } else {
                Log.w(TAG, "setupStoreTypesObserver: Received empty store types list")
            }
        }

        Log.d(TAG, "setupStoreTypesObserver: Store types observer setup completed")
    }

    private fun setupStoreTypeSpinner(storeTypes: List<StoreTypesItem>) {
        Log.d(TAG, "setupStoreTypeSpinner: Setting up store type spinner with ${storeTypes.size} items")

        // Create a custom adapter to display just the name but hold the whole object
        val adapter = object : ArrayAdapter<StoreTypesItem>(
            this,
            android.R.layout.simple_spinner_item,
            storeTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).text = getItem(position)?.name ?: ""
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).text = getItem(position)?.name ?: ""
                return view
            }

            // Override toString to ensure proper display
            override fun getItem(position: Int): StoreTypesItem? {
                return super.getItem(position)
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        Log.d(TAG, "setupStoreTypeSpinner: Store type adapter created")

        // Set adapter to spinner
        binding.spinnerStoreType.adapter = adapter
        Log.d(TAG, "setupStoreTypeSpinner: Adapter set to spinner")

        // Set item selection listener
        binding.spinnerStoreType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = adapter.getItem(position)
                Log.d(TAG, "Store type selected: position=$position, item=${selectedItem?.name}, id=${selectedItem?.id}")

                if (selectedItem != null && selectedItem.id > 0) {
                    // Store the actual ID from the API, not just position
                    viewModel.storeTypeId.value = selectedItem.id
                    Log.d(TAG, "Set storeTypeId to ${selectedItem.id}")
                } else {
                    Log.d(TAG, "Default or null store type selected, not setting storeTypeId")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No store type selected")
            }
        }

        // Hide progress bar after setup
        binding.storeTypeProgressBar?.visibility = View.GONE
        Log.d(TAG, "setupStoreTypeSpinner: Store type spinner setup completed")
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners: Setting up province and city spinners")

        // Setup province spinner
        binding.spinnerProvince.adapter = provinceAdapter
        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "Province selected at position: $position")
                val provinceId = provinceAdapter.getProvinceId(position)
                if (provinceId != null) {
                    Log.d(TAG, "Setting province ID: $provinceId")
                    viewModel.provinceId.value = provinceId
                    Log.d(TAG, "Fetching cities for province ID: $provinceId")
                    viewModel.getCities(provinceId)

                    // Reset city selection when province changes
                    Log.d(TAG, "Clearing city adapter for new province selection")
                    cityAdapter.clear()
                    binding.spinnerCity.setSelection(0)
                } else {
                    Log.e(TAG, "Invalid province ID for position: $position")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No province selected")
            }
        }

        // Setup city spinner
        binding.spinnerCity.adapter = cityAdapter
        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "City selected at position: $position")
                val cityId = cityAdapter.getCityId(position)
                if (cityId != null) {
                    Log.d(TAG, "Setting city ID: $cityId")
                    viewModel.cityId.value = cityId
                    viewModel.selectedCityId = cityId
                } else {
                    Log.e(TAG, "Invalid city ID for position: $position")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No city selected")
            }
        }

        // Add initial hints to the spinners
        if (provinceAdapter.isEmpty) {
            Log.d(TAG, "Adding default province hint")
            provinceAdapter.add("Pilih Provinsi")
        }

        if (cityAdapter.isEmpty) {
            Log.d(TAG, "Adding default city hint")
            cityAdapter.add("Pilih Kabupaten/Kota")
        }

        Log.d(TAG, "setupSpinners: Province and city spinners setup completed")
    }

    private fun setupDocumentUploads() {
        Log.d(TAG, "setupDocumentUploads: Setting up document upload buttons")

        // Store Image
        binding.containerStoreImg.setOnClickListener {
            Log.d(TAG, "Store image container clicked, picking image")
            pickImage(PICK_STORE_IMAGE_REQUEST)
        }

        // KTP
        binding.containerKtp.setOnClickListener {
            Log.d(TAG, "KTP container clicked, picking image")
            pickImage(PICK_KTP_REQUEST)
        }

        // NIB
        binding.containerNib.setOnClickListener {
            Log.d(TAG, "NIB container clicked, picking document")
            pickDocument(PICK_NIB_REQUEST)
        }

        // NPWP
        binding.containerNpwp?.setOnClickListener {
            Log.d(TAG, "NPWP container clicked, picking image")
            pickImage(PICK_NPWP_REQUEST)
        }

        // SPPIRT
        binding.containerSppirt.setOnClickListener {
            Log.d(TAG, "SPPIRT container clicked, picking document")
            pickDocument(PICK_PERSETUJUAN_REQUEST)
        }

        // Halal
        binding.containerHalal.setOnClickListener {
            Log.d(TAG, "Halal container clicked, picking document")
            pickDocument(PICK_QRIS_REQUEST)
        }

        Log.d(TAG, "setupDocumentUploads: Document upload buttons setup completed")
    }

    private fun pickImage(requestCode: Int) {
        Log.d(TAG, "pickImage: Launching image picker with request code: $requestCode")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    private fun pickDocument(requestCode: Int) {
        Log.d(TAG, "pickDocument: Launching document picker with request code: $requestCode")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, requestCode)
    }

    private fun setupCourierSelection() {
        Log.d(TAG, "setupCourierSelection: Setting up courier checkboxes")

        binding.checkboxJne.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "JNE checkbox ${if (isChecked) "checked" else "unchecked"}")
            handleCourierSelection("jne", isChecked)
        }

        binding.checkboxJnt.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "JNT checkbox ${if (isChecked) "checked" else "unchecked"}")
            handleCourierSelection("tiki", isChecked)
        }

        binding.checkboxPos.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "POS checkbox ${if (isChecked) "checked" else "unchecked"}")
            handleCourierSelection("pos", isChecked)
        }

        Log.d(TAG, "setupCourierSelection: Courier checkboxes setup completed")
    }

    private fun handleCourierSelection(courier: String, isSelected: Boolean) {
        if (isSelected) {
            if (!viewModel.selectedCouriers.contains(courier)) {
                viewModel.selectedCouriers.add(courier)
                Log.d(TAG, "handleCourierSelection: Added courier: $courier. Current couriers: ${viewModel.selectedCouriers}")
            }
        } else {
            viewModel.selectedCouriers.remove(courier)
            Log.d(TAG, "handleCourierSelection: Removed courier: $courier. Current couriers: ${viewModel.selectedCouriers}")
        }
    }

    private fun setupMap() {
        Log.d(TAG, "setupMap: Setting up map container")
        // This would typically integrate with Google Maps SDK
        // For simplicity, we're just using a placeholder
        binding.mapContainer.setOnClickListener {
            Log.d(TAG, "Map container clicked, checking location permission")
            // Request location permission if not granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Location permission not granted, requesting permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST
                )
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
                Log.d(TAG, "Location permission granted, setting default location")
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Default location set - Lat: ${viewModel.latitude.value}, Long: ${viewModel.longitude.value}")
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Location permission already granted, setting location")
                // Show map selection UI
                // This would typically launch Maps UI for location selection
                // For now, we'll just set some dummy coordinates
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
                Log.d(TAG, "Location set - Lat: ${viewModel.latitude.value}, Long: ${viewModel.longitude.value}")
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d(TAG, "setupMap: Map container setup completed")
    }

    private fun setupDataBinding() {
        Log.d(TAG, "setupDataBinding: Setting up two-way data binding for text fields")

        // Two-way data binding for text fields
        binding.etStoreName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.storeName.value = s.toString()
                Log.d(TAG, "Store name updated: ${s.toString()}")
            }
        })

        binding.etStoreDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.storeDescription.value = s.toString()
                Log.d(TAG, "Store description updated: ${s.toString().take(20)}${if ((s?.length ?: 0) > 20) "..." else ""}")
            }
        })

        binding.etStreet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.street.value = s.toString()
                Log.d(TAG, "Street address updated: ${s.toString()}")
            }
        })

        binding.etPostalCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    viewModel.postalCode.value = s.toString().toInt()
                    Log.d(TAG, "Postal code updated: ${s.toString()}")
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                    Log.e(TAG, "Invalid postal code input: ${s.toString()}, error: $e")
                }
            }
        })

        binding.etAddressDetail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.addressDetail.value = s.toString()
                Log.d(TAG, "Address detail updated: ${s.toString()}")
            }
        })

        binding.etBankNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    try {
                        viewModel.bankNumber.value = input.toInt()
                        Log.d(TAG, "Bank number updated: $input")
                    } catch (e: NumberFormatException) {
                        // Handle invalid input if needed
                        Log.e(TAG, "Failed to parse bank number. Input: $input, Error: $e")
                    }
                } else {
                    // Handle empty input - perhaps set to 0 or null depending on your requirements
                    viewModel.bankNumber.value = 0 // or 0
                    Log.d(TAG, "Bank number set to default: 0")
                }
            }
        })

        binding.etSubdistrict.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.subdistrict.value = s.toString()
                Log.d(TAG, "Subdistrict updated: ${s.toString()}")
            }
        })

        binding.etBankName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.bankName.value = s.toString()
                Log.d(TAG, "Bank name updated: ${s.toString()}")
            }
        })

        Log.d(TAG, "setupDataBinding: Text field data binding setup completed")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: Request code: $requestCode, Result code: $resultCode")
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            Log.d(TAG, "onActivityResult: URI received: $uri")
            when (requestCode) {
                PICK_STORE_IMAGE_REQUEST -> {
                    Log.d(TAG, "Store image selected")
                    viewModel.storeImageUri = uri
                    updateImagePreview(uri, binding.imgStore, binding.layoutUploadStoreImg)
                }
                PICK_KTP_REQUEST -> {
                    Log.d(TAG, "KTP image selected")
                    viewModel.ktpUri = uri
                    updateImagePreview(uri, binding.imgKtp, binding.layoutUploadKtp)
                }
                PICK_NPWP_REQUEST -> {
                    Log.d(TAG, "NPWP document selected")
                    viewModel.npwpUri = uri
                    updateDocumentPreview(binding.layoutUploadNpwp)
                }
                PICK_NIB_REQUEST -> {
                    Log.d(TAG, "NIB document selected")
                    viewModel.nibUri = uri
                    updateDocumentPreview(binding.layoutUploadNib)
                }
                PICK_PERSETUJUAN_REQUEST -> {
                    Log.d(TAG, "SPPIRT document selected")
                    viewModel.persetujuanUri = uri
                    updateDocumentPreview(binding.layoutUploadSppirt)
                }
                PICK_QRIS_REQUEST -> {
                    Log.d(TAG, "Halal document selected")
                    viewModel.qrisUri = uri
                    updateDocumentPreview(binding.layoutUploadHalal)
                }
                else -> {
                    Log.w(TAG, "Unknown request code: $requestCode")
                }
            }
        } else {
            Log.w(TAG, "File selection canceled or failed")
        }
    }

    private fun updateImagePreview(uri: Uri?, imageView: ImageView, uploadLayout: LinearLayout) {
        uri?.let {
            Log.d(TAG, "updateImagePreview: Setting image URI: $uri")
            imageView.setImageURI(it)
            imageView.visibility = View.VISIBLE
            uploadLayout.visibility = View.GONE
        }
    }

    private fun updateDocumentPreview(uploadLayout: LinearLayout) {
        Log.d(TAG, "updateDocumentPreview: Updating document preview UI")
        // For documents, we just show a success indicator
        val checkIcon = ImageView(this)
        checkIcon.setImageResource(android.R.drawable.ic_menu_gallery)
        val successText = TextView(this)
        successText.text = "Dokumen berhasil diunggah"

        uploadLayout.removeAllViews()
        uploadLayout.addView(checkIcon)
        uploadLayout.addView(successText)
        Log.d(TAG, "updateDocumentPreview: Document preview updated with success indicator")
    }

    //later implement get location form gps
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location selection
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // Show loading indicator
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Mendaftar..."
        } else {
            // Hide loading indicator
            binding.btnRegister.isEnabled = true
            binding.btnRegister.text = "Daftar"
        }
    }

    companion object {
        private const val TAG = "RegisterStoreActivity"
    }
}