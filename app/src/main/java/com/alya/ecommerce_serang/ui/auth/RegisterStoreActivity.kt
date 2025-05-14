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
        binding = ActivityRegisterStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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

        provinceAdapter = ProvinceAdapter(this)
        cityAdapter = CityAdapter(this)

        setupDataBinding()
        setupSpinners() // Location spinners

        // Setup observers
        setupStoreTypesObserver() // Store type observer
        setupObservers()

        setupMap()
        setupDocumentUploads()
        setupCourierSelection()

        viewModel.fetchStoreTypes()
        viewModel.getProvinces()


        // Setup register button
        binding.btnRegister.setOnClickListener {
            if (viewModel.validateForm()) {
                viewModel.registerStore(this)
            } else {
                Toast.makeText(this, "Harap lengkapi semua field yang wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        // Observe province state
        viewModel.provincesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "Loading provinces...")
                    binding.provinceProgressBar?.visibility = View.VISIBLE
                    binding.spinnerProvince.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "Provinces loaded: ${state.data.size}")
                    binding.provinceProgressBar?.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true

                    // Update adapter with data
                    provinceAdapter.updateData(state.data)
                }
                is Result.Error -> {
//                    Log.e(TAG, "Error loading provinces: ${state.}")
                    binding.provinceProgressBar?.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true

//                    Toast.makeText(this, "Gagal memuat provinsi: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe city state
        viewModel.citiesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "Loading cities...")
                    binding.cityProgressBar?.visibility = View.VISIBLE
                    binding.spinnerCity.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "Cities loaded: ${state.data.size}")
                    binding.cityProgressBar?.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true

                    // Update adapter with data
                    cityAdapter.updateData(state.data)
                }
                is Result.Error -> {
//                    Log.e(TAG, "Error loading cities: ${state.message}")
                    binding.cityProgressBar?.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true

//                    Toast.makeText(this, "Gagal memuat kota: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe registration state
        viewModel.registerState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Toko berhasil didaftarkan", Toast.LENGTH_SHORT).show()
                    finish() // Return to previous screen
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Gagal mendaftarkan toko: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupStoreTypesObserver() {
        // Observe loading state
        viewModel.isLoadingType.observe(this) { isLoading ->
            if (isLoading) {
                // Show loading indicator for store types spinner
                binding.spinnerStoreType.isEnabled = false
                binding.storeTypeProgressBar?.visibility = View.VISIBLE
            } else {
                binding.spinnerStoreType.isEnabled = true
                binding.storeTypeProgressBar?.visibility = View.GONE
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Toast.makeText(this, "Error loading store types: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe store types data
        viewModel.storeTypes.observe(this) { storeTypes ->
            Log.d(TAG, "Store types loaded: ${storeTypes.size}")
            if (storeTypes.isNotEmpty()) {
                // Add "Pilih Jenis UMKM" as the first item if it's not already there
                val displayList = if (storeTypes.any { it.name == "Pilih Jenis UMKM" || it.id == 0 }) {
                    storeTypes
                } else {
                    val defaultItem = StoreTypesItem(name = "Pilih Jenis UMKM", id = 0)
                    listOf(defaultItem) + storeTypes
                }

                // Setup spinner with API data
                setupStoreTypeSpinner(displayList)
            }
        }
    }

    private fun setupStoreTypeSpinner(storeTypes: List<StoreTypesItem>) {
        Log.d(TAG, "Setting up store type spinner with ${storeTypes.size} items")

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

        // Set adapter to spinner
        binding.spinnerStoreType.adapter = adapter

        // Set item selection listener
        binding.spinnerStoreType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = adapter.getItem(position)
                Log.d(TAG, "Store type selected: position=$position, item=${selectedItem?.name}, id=${selectedItem?.id}")

                if (selectedItem != null && selectedItem.id > 0) {
                    // Store the actual ID from the API, not just position
                    viewModel.storeTypeId.value = selectedItem.id
                    Log.d(TAG, "Set storeTypeId to ${selectedItem.id}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No store type selected")
            }
        }

        // Hide progress bar after setup
        binding.storeTypeProgressBar?.visibility = View.GONE
    }

    private fun setupSpinners() {
        // Setup province spinner
        binding.spinnerProvince.adapter = provinceAdapter
        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "Province selected at position: $position")
                val provinceId = provinceAdapter.getProvinceId(position)
                if (provinceId != null) {
                    Log.d(TAG, "Setting province ID: $provinceId")
                    viewModel.provinceId.value = provinceId
                    viewModel.getCities(provinceId)

                    // Reset city selection when province changes
                    cityAdapter.clear()
                    binding.spinnerCity.setSelection(0)
                } else {
                    Log.e(TAG, "Invalid province ID for position: $position")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
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
                // Do nothing
            }
        }

        // Add initial hints to the spinners
        if (provinceAdapter.isEmpty) {
            provinceAdapter.add("Pilih Provinsi")
        }

        if (cityAdapter.isEmpty) {
            cityAdapter.add("Pilih Kabupaten/Kota")
        }
    }

//    private fun setupSubdistrictSpinner(cityId: Int) {
//        // This would typically be populated from API based on cityId
//        val subdistricts = listOf("Pilih Kecamatan", "Kecamatan 1", "Kecamatan 2", "Kecamatan 3")
//        val subdistrictAdapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, subdistricts)
//        binding.spinnerSubdistrict.adapter = subdistrictAdapter
//        binding.spinnerSubdistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                if (position > 0) {
//                    viewModel.subdistrict.value = subdistricts[position]
//                }
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
//    }

    private fun setupDocumentUploads() {
        // Store Image
        binding.containerStoreImg.setOnClickListener {
            pickImage(PICK_STORE_IMAGE_REQUEST)
        }

        // KTP
        binding.containerKtp.setOnClickListener {
            pickImage(PICK_KTP_REQUEST)
        }

        // NIB
        binding.containerNib.setOnClickListener {
            pickDocument(PICK_NIB_REQUEST)
        }

        // NPWP
        binding.containerNpwp?.setOnClickListener {
            pickImage(PICK_NPWP_REQUEST)
        }

        // SPPIRT
        binding.containerSppirt.setOnClickListener {
            pickDocument(PICK_PERSETUJUAN_REQUEST)
        }

        // Halal
        binding.containerHalal.setOnClickListener {
            pickDocument(PICK_QRIS_REQUEST)
        }
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    private fun pickDocument(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, requestCode)
    }

    private fun setupCourierSelection() {
        binding.checkboxJne.setOnCheckedChangeListener { _, isChecked ->
            handleCourierSelection("jne", isChecked)
        }

        binding.checkboxJnt.setOnCheckedChangeListener { _, isChecked ->
            handleCourierSelection("jnt", isChecked)
        }

        binding.checkboxPos.setOnCheckedChangeListener { _, isChecked ->
            handleCourierSelection("pos", isChecked)
        }
    }

    private fun handleCourierSelection(courier: String, isSelected: Boolean) {
        if (isSelected) {
            if (!viewModel.selectedCouriers.contains(courier)) {
                viewModel.selectedCouriers.add(courier)
            }
        } else {
            viewModel.selectedCouriers.remove(courier)
        }
    }

    private fun setupMap() {
        // This would typically integrate with Google Maps SDK
        // For simplicity, we're just using a placeholder
        binding.mapContainer.setOnClickListener {
            // Request location permission if not granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST

                )
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
            } else {
                // Show map selection UI
                // This would typically launch Maps UI for location selection
                // For now, we'll just set some dummy coordinates
                viewModel.latitude.value = "-6.2088"
                viewModel.longitude.value = "106.8456"
                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDataBinding() {
        // Two-way data binding for text fields
        binding.etStoreName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.storeName.value = s.toString()
            }
        })

        binding.etStoreDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.storeDescription.value = s.toString()
            }
        })

        binding.etStreet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.street.value = s.toString()
            }
        })

        binding.etPostalCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    viewModel.postalCode.value = s.toString().toInt()
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                    //show toast
                }
            }
        })

        binding.etAddressDetail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.addressDetail.value = s.toString()
            }
        })

        binding.etBankNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.bankNumber.value = s.toString().toInt()
            }
        })

        binding.etSubdistrict.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.subdistrict.value = s.toString()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            when (requestCode) {
                PICK_STORE_IMAGE_REQUEST -> {
                    viewModel.storeImageUri = uri
                    updateImagePreview(uri, binding.imgStore, binding.layoutUploadStoreImg)
                }
                PICK_KTP_REQUEST -> {
                    viewModel.ktpUri = uri
                    updateImagePreview(uri, binding.imgKtp, binding.layoutUploadKtp)
                }
                PICK_NPWP_REQUEST -> {
                    viewModel.npwpUri = uri
                    updateDocumentPreview(binding.layoutUploadNpwp)
                }
                PICK_NIB_REQUEST -> {
                    viewModel.nibUri = uri
                    updateDocumentPreview(binding.layoutUploadNib)
                }
                PICK_PERSETUJUAN_REQUEST -> {
                    viewModel.persetujuanUri = uri
                    updateDocumentPreview(binding.layoutUploadSppirt)
                }
                PICK_QRIS_REQUEST -> {
                    viewModel.qrisUri = uri
                    updateDocumentPreview(binding.layoutUploadHalal)
                }
            }
        }
    }

    private fun updateImagePreview(uri: Uri?, imageView: ImageView, uploadLayout: LinearLayout) {
        uri?.let {
            imageView.setImageURI(it)
            imageView.visibility = View.VISIBLE
            uploadLayout.visibility = View.GONE
        }
    }

    private fun updateDocumentPreview(uploadLayout: LinearLayout) {
        // For documents, we just show a success indicator
        val checkIcon = ImageView(this)
        checkIcon.setImageResource(android.R.drawable.ic_menu_gallery)
        val successText = TextView(this)
        successText.text = "Dokumen berhasil diunggah"

        uploadLayout.removeAllViews()
        uploadLayout.addView(checkIcon)
        uploadLayout.addView(successText)
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