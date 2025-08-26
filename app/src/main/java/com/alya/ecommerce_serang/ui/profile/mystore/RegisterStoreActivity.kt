package com.alya.ecommerce_serang.ui.profile.mystore

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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.PaymentUpdate
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityRegisterStoreBinding
import com.alya.ecommerce_serang.ui.order.address.BankAdapter
import com.alya.ecommerce_serang.ui.order.address.CityAdapter
import com.alya.ecommerce_serang.ui.order.address.ProvinceAdapter
import com.alya.ecommerce_serang.ui.order.address.SubdsitrictAdapter
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.FileUtils
import com.alya.ecommerce_serang.utils.ImageUtils
import com.alya.ecommerce_serang.utils.PopUpDialog
import com.alya.ecommerce_serang.utils.RegisterStoreViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.alya.ecommerce_serang.utils.viewmodel.RegisterStoreViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RegisterStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterStoreBinding
    private lateinit var sessionManager: SessionManager

    private lateinit var provinceAdapter: ProvinceAdapter
    private lateinit var cityAdapter: CityAdapter
    private lateinit var subdistrictAdapter: SubdsitrictAdapter
    private lateinit var bankAdapter: BankAdapter

    // pending values (filled from myStoreProfile once)
    private var wantedProvinceId: Int? = null
    private var wantedCityId: String? = null
    private var wantedSubdistrictId: String? = null
    private var wantedBankName: String? = null

    // one-shot guards so we don't re-apply repeatedly
    private var provinceApplied = false
    private var cityApplied = false
    private var subdistrictApplied = false
    private var bankApplied = false

    // avoid clearing/overriding while restoring
    private var isRestoringSelections = false

    // Request codes for file picking
    private val PICK_STORE_IMAGE_REQUEST = 1001
    private val PICK_KTP_REQUEST = 1002
    private val PICK_NPWP_REQUEST = 1003
    private val PICK_NIB_REQUEST = 1004
    private var isReapply: Boolean = false

    // Location request code
    private val LOCATION_PERMISSION_REQUEST = 2001

    private val viewModel: RegisterStoreViewModel by viewModels {
        RegisterStoreViewModelFactory(this, intent.extras)
    }

    private val myStoreViewModel: MyStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val myStoreRepository = MyStoreRepository(apiService)
            MyStoreViewModel(myStoreRepository)
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

        setupHeader()

        isReapply = intent.getBooleanExtra("REAPPLY", false)

        provinceAdapter = ProvinceAdapter(this)
        cityAdapter = CityAdapter(this)
        subdistrictAdapter = SubdsitrictAdapter(this)
        bankAdapter = BankAdapter(this)
        Log.d(TAG, "onCreate: Adapters initialized")

        setupDataBinding()
        Log.d(TAG, "onCreate: Data binding setup completed")

        setupSpinners() // Location spinners
        Log.d(TAG, "onCreate: Spinners setup completed")

        binding.checkboxApprove.setOnCheckedChangeListener { _, _ ->
            validateRequiredFields()
        }

        // Setup observers
        setupStoreTypesObserver() // Store type observer
        setupObservers()
        Log.d(TAG, "onCreate: Observers setup completed")

        viewModel.latitude.value = "-6.2088"
        viewModel.longitude.value = "106.8456"
        Log.d(TAG, "Location permission granted, setting default location")
//        Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Default location set - Lat: ${viewModel.latitude.value}, Long: ${viewModel.longitude.value}")
//        Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()

        setupDocumentUploads()
        Log.d(TAG, "onCreate: Document uploads setup completed")

        setupCourierSelection()
        Log.d(TAG, "onCreate: Courier selection setup completed")

        Log.d(TAG, "onCreate: Fetching store types from API")
        viewModel.fetchStoreTypes()

        Log.d(TAG, "onCreate: Fetching provinces from API")
        viewModel.getProvinces()

        viewModel.provinceId.observe(this) { validateRequiredFields() }
        viewModel.cityId.observe(this) { validateRequiredFields() }
        viewModel.storeTypeId.observe(this) { validateRequiredFields() }

        if (isReapply) {
            binding.btnRegister.text = "Ajukan Kembali"
            binding.layoutRejected.visibility = View.VISIBLE

            myStoreViewModel.loadMyStore()

            myStoreViewModel.myStoreProfile.observe(this) { storeDataResponse ->
                storeDataResponse?.let { storeResponse ->
                    val store = storeResponse.store
                    binding.tvRejectedReason.text = store.approvalReason

                    // Prefill basic fields
                    binding.etStoreName.setText(store.storeName)
                    binding.etStoreDescription.setText(store.storeDescription)
                    binding.etStreet.setText(store.street)
                    binding.etPostalCode.setText(store.postalCode)
                    binding.etAddressDetail.setText(store.detail)

                    viewModel.storeName.value = store.storeName
                    viewModel.storeDescription.value = store.storeDescription
                    viewModel.street.value = store.street
                    viewModel.postalCode.value = store.postalCode.toIntOrNull() ?: 0
                    viewModel.addressDetail.value = store.detail

                    // Prefill bank info
                    storeResponse.payment.firstOrNull()?.let { payment ->
                        viewModel.bankName.value = payment.bankName
                        viewModel.bankNumber.value = payment.bankNum.toIntOrNull() ?: 0
                        val bankPosition = bankAdapter.findPositionByName(payment.bankName)
                        binding.spinnerBankName.setSelection(bankPosition, false)
                    }

                    // Prefill couriers
                    storeResponse.shipping.forEach { courier ->
                        when (courier.courier) {
                            "jne" -> binding.checkboxJne.isChecked = true
                            "pos" -> binding.checkboxPos.isChecked = true
                            "tiki" -> binding.checkboxTiki.isChecked = true
                        }
                    }

                    // Prefill document URIs
                    store.ktp.let { ktpUri ->
                        viewModel.ktpUri = ktpUri.toUri()
                        updateImagePreview(viewModel.ktpUri, binding.imgKtp, binding.layoutUploadKtp)
                    }
                    store.npwp.let { npwpUri ->
                        viewModel.npwpUri = npwpUri.toUri()
                        updateDocumentPreview(binding.layoutUploadNpwp)
                    }
                    store.nib.let { nibUri ->
                        viewModel.nibUri = nibUri.toUri()
                        updateDocumentPreview(binding.layoutUploadNib)
                    }

                    // Prefill spinner for store types
                    preselectStoreType(store.storeTypeId)

                    // Cache what we want to select later (after data arrives)
                    wantedProvinceId    = store.provinceId
                    wantedCityId        = store.cityId
                    wantedSubdistrictId = store.subdistrict
                    wantedBankName      = storeResponse.payment.firstOrNull()?.bankName

                    // Cache what we want to select later (after data arrives)
                    wantedProvinceId    = store.provinceId
                    wantedCityId        = store.cityId
                    wantedSubdistrictId = store.subdistrict
                    wantedBankName      = storeResponse.payment.firstOrNull()?.bankName

                    // Mark restoring flow on
                    isRestoringSelections = true

                    // Try to apply immediately (if adapters already have data), otherwise
                    // observers below will apply when data is ready.
                    tryApplyProvince()
                    tryApplyCity()
                    tryApplySubdistrict()
                    tryApplyBank()

                    validateRequiredFields()
                }
            }

            binding.btnRegister.setOnClickListener {
                doUpdateStoreProfile()
            }
        } else {
            binding.btnRegister.setOnClickListener {
                if (viewModel.validateForm()){
                    PopUpDialog.showConfirmDialog(
                        context = this,
                        title = "Apakah anda yakin ingin mendaftar toko?",
                        message = "Pastikan data yang dimasukkan sudah benar",
                        positiveText = "Ya",
                        negativeText = "Tidak",
                        onYesClicked = {
                            viewModel.registerStore(this)
                        }
                    )
                }
                else {
                    Toast.makeText(this, "Harap lengkapi semua field yang wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
        }
        validateRequiredFields()
    }

    private fun preselectStoreType(storeTypeId: Int) {
        // The adapter is created in setupStoreTypeSpinner(...)
        val adapter = binding.spinnerStoreType.adapter ?: return
        for (i in 0 until adapter.count) {
            val item = adapter.getItem(i) as? StoreTypesItem
            if (item?.id == storeTypeId) {
                binding.spinnerStoreType.setSelection(i, false)
                viewModel.storeTypeId.value = storeTypeId
                validateRequiredFields()
                break
            }
        }
    }

    private fun tryApplyProvince() {
        if (provinceApplied) return
        val target = wantedProvinceId ?: return
        val count = provinceAdapter.count
        for (i in 0 until count) {
            if (provinceAdapter.getProvinceId(i) == target) {
                binding.spinnerProvince.setSelection(i, false)
                provinceApplied = true
                maybeFinishRestoring()
                return
            }
        }
    }

    private fun tryApplyCity() {
        if (cityApplied) return
        val target = wantedCityId ?: return
        val count = cityAdapter.count
        for (i in 0 until count) {
            if (cityAdapter.getCityId(i) == target) {
                binding.spinnerCity.setSelection(i, false)
                cityApplied = true
                maybeFinishRestoring()
                return
            }
        }
    }

    private fun tryApplySubdistrict() {
        if (subdistrictApplied) return
        val target = wantedSubdistrictId ?: return
        val count = subdistrictAdapter.count
        for (i in 0 until count) {
            if (subdistrictAdapter.getSubdistrictId(i) == target) {
                binding.spinnerSubdistrict.setSelection(i, false)
                subdistrictApplied = true
                maybeFinishRestoring()
                return
            }
        }
    }

    private fun tryApplyBank() {
        if (bankApplied) return
        val targetName = wantedBankName ?: return
        val pos = bankAdapter.findPositionByName(targetName)
        if (pos >= 0) {
            binding.spinnerBankName.setSelection(pos, false)
            viewModel.bankName.value = targetName
            viewModel.selectedBankName = targetName
            validateRequiredFields()
            bankApplied = true
        }
    }

    private fun setupHeader() {
        binding.header.main.background = ContextCompat.getColor(this, R.color.blue_500).toDrawable()
        binding.header.headerTitle.visibility = View.GONE
        binding.header.headerLeftIcon.setColorFilter(
            ContextCompat.getColor(this, R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    private fun validateRequiredFields() {
        val bankName = viewModel.bankName.value?.trim().orEmpty()
        val bankSelected = bankName.isNotEmpty() && !bankName.equals("Pilih Bank", ignoreCase = true)

        val provinceSelected    = viewModel.provinceId.value != null
        val citySelected        = !viewModel.cityId.value.isNullOrBlank()
        val subdistrictSelected = !viewModel.subdistrict.value.isNullOrBlank()

        val currentStoreType = binding.spinnerStoreType.selectedItem as? StoreTypesItem
        val storeTypeSelected = when {
            currentStoreType != null -> currentStoreType.id != 0 &&
                    !currentStoreType.name.equals("Pilih Jenis UMKM", true)
            else -> (viewModel.storeTypeId.value ?: -1) > 0
        }

        val isFormValid =
            !viewModel.storeName.value.isNullOrBlank() &&
                    !viewModel.street.value.isNullOrBlank() &&
                    (viewModel.postalCode.value ?: 0) > 0 &&
                    provinceSelected && citySelected && subdistrictSelected &&
                    storeTypeSelected &&
                    bankSelected &&
                    (viewModel.bankNumber.value ?: 0) > 0 &&
                    viewModel.ktpUri != null &&
                    viewModel.nibUri != null &&
                    viewModel.npwpUri != null &&
                    viewModel.selectedCouriers.isNotEmpty() &&
                    !viewModel.accountName.value.isNullOrBlank() &&
                    binding.checkboxApprove.isChecked

        binding.btnRegister.isEnabled = isFormValid
        binding.btnRegister.setBackgroundResource(
            if (isFormValid) R.drawable.bg_button_active else R.drawable.bg_button_disabled
        )
        binding.btnRegister.setTextColor(
            ContextCompat.getColor(this, if (isFormValid) R.color.white else R.color.black_300)
        )
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up LiveData observers")

        // Observe province state
        viewModel.provincesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "setupObservers: Loading provinces...")
                    binding.provinceProgressBar.visibility = View.VISIBLE
                    binding.spinnerProvince.isEnabled = false
                }
                is Result.Success -> {
                    binding.provinceProgressBar.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true
                    provinceAdapter.updateData(state.data)
                    tryApplyProvince()
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Error loading provinces: ${state.exception.message}")
                    binding.provinceProgressBar.visibility = View.GONE
                    binding.spinnerProvince.isEnabled = true
                }
            }
        }

        // Observe city state
        viewModel.citiesState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "setupObservers: Loading cities...")
                    binding.cityProgressBar.visibility = View.VISIBLE
                    binding.spinnerCity.isEnabled = false
                }
                is Result.Success -> {
                    binding.cityProgressBar.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true
                    cityAdapter.updateData(state.data)
                    tryApplyCity()
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Error loading cities: ${state.exception.message}")
                    binding.cityProgressBar.visibility = View.GONE
                    binding.spinnerCity.isEnabled = true
                }
            }
        }

        viewModel.subdistrictState.observe(this) { state ->
            when (state) {
                is Result.Loading -> {
                    Log.d(TAG, "setupobservers: Loading Subdistrict...")
                    binding.subdistrictProgressBar.visibility = View.VISIBLE
                    binding.spinnerSubdistrict.isEnabled = false
                }
                is Result.Success -> {
                    binding.subdistrictProgressBar.visibility = View.GONE
                    binding.spinnerSubdistrict.isEnabled = true
                    subdistrictAdapter.updateData(state.data)

                    // If you’re not restoring a specific subdistrict, select the first real item
                    if (!isRestoringSelections && state.data.isNotEmpty()) {
                        binding.spinnerSubdistrict.setSelection(0, false)
                        val id0 = subdistrictAdapter.getSubdistrictId(0)
                        viewModel.subdistrict.value = id0 ?: ""
                        viewModel.selectedSubdistrict = id0
                        validateRequiredFields()
                    }

                    tryApplySubdistrict()
                }
                is Result.Error -> {
                    Log.e(TAG, "setupObservers: Error loading subdistrict: ${state.exception.message}")
                    binding.subdistrictProgressBar.visibility = View.GONE
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
                binding.storeTypeProgressBar.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "setupStoreTypesObserver: Store types loading completed")
                binding.spinnerStoreType.isEnabled = true
                binding.storeTypeProgressBar.visibility = View.GONE
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Log.e(TAG, "setupStoreTypesObserver: Error loading store types: $errorMsg")
//                Toast.makeText(this, "Error loading store types: $errorMsg", Toast.LENGTH_SHORT).show()
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
                tryApplyBank()
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val item = (binding.spinnerStoreType.adapter.getItem(pos) as? StoreTypesItem)
                if (item != null && item.id > 0) viewModel.storeTypeId.value = item.id
                validateRequiredFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No store type selected")
            }
        }

        // Hide progress bar after setup
        binding.storeTypeProgressBar.visibility = View.GONE
        Log.d(TAG, "setupStoreTypeSpinner: Store type spinner setup completed")
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners: Setting up province and city spinners")

        // Setup province spinner
        binding.spinnerProvince.adapter = provinceAdapter
        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                provinceAdapter.getProvinceId(pos)?.let {
                    viewModel.provinceId.value = it
                    viewModel.getCities(it)
                }
                validateRequiredFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No province selected")
            }
        }

        // Setup city spinner
        binding.spinnerCity.adapter = cityAdapter
        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                cityAdapter.getCityId(pos)?.let {
                    viewModel.cityId.value = it
                    viewModel.getSubdistrict(it)
                    viewModel.selectedCityId = it
                }
                validateRequiredFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No city selected")
            }
        }

        //Setup Subdistrict spinner
        binding.spinnerSubdistrict.adapter = subdistrictAdapter
        binding.spinnerSubdistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val selectedId = subdistrictAdapter.getSubdistrictId(pos)
                viewModel.subdistrict.value = selectedId ?: ""     // empty => not selected
                viewModel.selectedSubdistrict = selectedId
                validateRequiredFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No city selected")
            }
        }

        binding.spinnerBankName.adapter = bankAdapter
        binding.spinnerBankName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val bankName = bankAdapter.getBankName(pos)
                viewModel.bankName.value = bankName
                viewModel.selectedBankName = bankName
                validateRequiredFields()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { /* no-op */ }
        }
        tryApplyBank()

//        // Add initial hints to the spinners
//        if (provinceAdapter.isEmpty)    provinceAdapter.add("Pilih Provinsi")
//        if (cityAdapter.isEmpty)        cityAdapter.add("Pilih Kabupaten/Kota")
//        if (subdistrictAdapter.isEmpty) subdistrictAdapter.add("Pilih Kecamatan")
//        if (bankAdapter.isEmpty)        bankAdapter.add("Pilih Bank")

        Log.d(TAG, "setupSpinners: Province and city spinners setup completed")
    }

    private fun maybeFinishRestoring() {
        if (provinceApplied && cityApplied && subdistrictApplied) {
            isRestoringSelections = false
        }
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
        binding.containerNpwp.setOnClickListener {
            Log.d(TAG, "NPWP container clicked, picking image")
            pickImage(PICK_NPWP_REQUEST)
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

        binding.checkboxPos.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "POS checkbox ${if (isChecked) "checked" else "unchecked"}")
            handleCourierSelection("pos", isChecked)
        }

        binding.checkboxTiki.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "TIKI checkbox ${if (isChecked) "checked" else "unchecked"}")
            handleCourierSelection("tiki", isChecked)
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
        validateRequiredFields()
    }

//    private fun setupMap() {
//        Log.d(TAG, "setupMap: Setting up map container")
//        // This would typically integrate with Google Maps SDK
//        // For simplicity, we're just using a placeholder
//        binding.mapContainer.setOnClickListener {
//            Log.d(TAG, "Map container clicked, checking location permission")
//            // Request location permission if not granted
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                Log.d(TAG, "Location permission not granted, requesting permission")
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                    LOCATION_PERMISSION_REQUEST
//                )
//                viewModel.latitude.value = "-6.2088"
//                viewModel.longitude.value = "106.8456"
//                Log.d(TAG, "Location permission granted, setting default location")
//                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
//                Log.d(TAG, "Default location set - Lat: ${viewModel.latitude.value}, Long: ${viewModel.longitude.value}")
//                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
//            } else {
//                Log.d(TAG, "Location permission already granted, setting location")
//                // Show map selection UI
//                // This would typically launch Maps UI for location selection
//                // For now, we'll just set some dummy coordinates
//                viewModel.latitude.value = "-6.2088"
//                viewModel.longitude.value = "106.8456"
//                Log.d(TAG, "Location set - Lat: ${viewModel.latitude.value}, Long: ${viewModel.longitude.value}")
//                Toast.makeText(this, "Lokasi dipilih", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        Log.d(TAG, "setupMap: Map container setup completed")
//    }

    private fun setupDataBinding() {
        Log.d(TAG, "setupDataBinding: Setting up two-way data binding for text fields")

        // Two-way data binding for text fields
        binding.etStoreName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.storeName.value != s.toString()) {
                    viewModel.storeName.value = s.toString()
                }
                Log.d(TAG, "Store name updated: ${s.toString()}")
                validateRequiredFields()
            }
        })

        viewModel.storeName.observe(this) { value ->
            if (binding.etStoreName.text.toString() != value) {
                binding.etStoreName.setText(value)
                binding.etStoreName.setSelection(value.length) // Set cursor to end
            }
        }

        binding.etStoreDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.storeDescription.value = s.toString()
                Log.d(TAG, "Store description updated: ${s.toString().take(20)}${if ((s?.length ?: 0) > 20) "..." else ""}")
                validateRequiredFields()
            }
        })

        binding.etStreet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.street.value != s.toString()) {
                    viewModel.street.value = s.toString()
                }
                Log.d(TAG, "Street address updated: ${s.toString()}")
                validateRequiredFields()
            }
        })

        viewModel.street.observe(this) { value ->
            if (binding.etStreet.text.toString() != value) {
                binding.etStreet.setText(value)
                binding.etStreet.setSelection(value.length)
            }
        }

        binding.etPostalCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newValue = s.toString().toIntOrNull() ?: 0
                if (viewModel.postalCode.value != newValue) {
                    viewModel.postalCode.value = newValue
                }
                validateRequiredFields()
            }
        })

        viewModel.postalCode.observe(this) { value ->
            val currentText = binding.etPostalCode.text.toString()
            val valueString = if (value == 0) "" else value.toString()
            if (currentText != valueString) {
                binding.etPostalCode.setText(valueString)
                binding.etPostalCode.setSelection(valueString.length)
            }
        }

        binding.etAddressDetail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.addressDetail.value != s.toString()) {
                    viewModel.addressDetail.value = s.toString()
                }
                Log.d(TAG, "Address detail updated: ${s.toString()}")
                validateRequiredFields()
            }
        })

        viewModel.addressDetail.observe(this) { value ->
            if (binding.etAddressDetail.text.toString() != value) {
                binding.etAddressDetail.setText(value)
                binding.etAddressDetail.setSelection(value.length)
            }
        }

        binding.etBankNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val newValue = if (input.isNotEmpty()) {
                    try {
                        input.toInt()
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Failed to parse bank number. Input: $input, Error: $e")
                        0
                    }
                } else {
                    0
                }

                if (viewModel.bankNumber.value != newValue) {
                    viewModel.bankNumber.value = newValue
                    Log.d(TAG, "Bank number updated: $newValue")
                }
                validateRequiredFields()
            }
        })
//
//        binding.etSubdistrict.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                viewModel.subdistrict.value = s.toString()
//                Log.d(TAG, "Subdistrict updated: ${s.toString()}")
//                validateRequiredFields()
//            }
//        })

//        binding.etBankName.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                viewModel.bankName.value = s.toString()
//                Log.d(TAG, "Bank name updated: ${s.toString()}")
//                validateRequiredFields()
//            }
//        })

        binding.etAccountName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.accountName.value != s.toString()) {
                    viewModel.accountName.value = s.toString()
                }
                Log.d(TAG, "Account Name updated: ${s.toString()}")
                validateRequiredFields()
            }

        })

        viewModel.accountName.observe(this) { value ->
            if (binding.etAccountName.text.toString() != value) {
                binding.etAccountName.setText(value)
                binding.etAccountName.setSelection(value.length)
            }
        }

        Log.d(TAG, "setupDataBinding: Text field data binding setup completed")
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: Request code: $requestCode, Result code: $resultCode")
        if (resultCode == RESULT_OK && data != null) {
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
                    validateRequiredFields()
                }
                PICK_NPWP_REQUEST -> {
                    Log.d(TAG, "NPWP document selected")
                    viewModel.npwpUri = uri
                    updateDocumentPreview(binding.layoutUploadNpwp)
                    validateRequiredFields()
                }
                PICK_NIB_REQUEST -> {
                    Log.d(TAG, "NIB document selected")
                    viewModel.nibUri = uri
                    updateDocumentPreview(binding.layoutUploadNib)
                    validateRequiredFields()
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

    private fun doUpdateStoreProfile() {
        // --- Text parts ---
        val nameBody: RequestBody        = (viewModel.storeName.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody: RequestBody        = ((viewModel.storeTypeId.value ?: 0).toString()).toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody: RequestBody        = (viewModel.storeDescription.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        // NOTE: is_on_leave is NOT part of approval multipart; keep separate if needed

        val latBody: RequestBody         = (viewModel.latitude.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val longBody: RequestBody        = (viewModel.longitude.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val provBody: RequestBody        = ((viewModel.provinceId.value ?: 0).toString()).toRequestBody("text/plain".toMediaTypeOrNull())
        val cityBody: RequestBody        = (viewModel.cityId.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val subdistrictBody: RequestBody = (viewModel.selectedSubdistrict ?: viewModel.subdistrict.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

        // If you don't have village picker yet, send empty string or reuse subdistrict
        val villageBody: RequestBody     = "".toRequestBody("text/plain".toMediaTypeOrNull())

        val streetBody: RequestBody      = (viewModel.street.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val postalBody: RequestBody      = ((viewModel.postalCode.value ?: 0).toString()).toRequestBody("text/plain".toMediaTypeOrNull())
        val detailBody: RequestBody      = (viewModel.addressDetail.value ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

        // You can read user phone from current store profile when reapply
        val currentPhone = myStoreViewModel.myStoreProfile.value?.store?.userPhone ?: ""
        val userPhoneBody: RequestBody   = currentPhone.toRequestBody("text/plain".toMediaTypeOrNull())

        // --- Multipart images/docs (safe compress/copy) ---
        val storeImgPart: MultipartBody.Part? = viewModel.storeImageUri?.let { uri ->
            try {
                val allowed = Regex("^(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)
                if (!ImageUtils.isAllowedFileType(this, uri, allowed)) {
                    Toast.makeText(this, "Format gambar tidak didukung", Toast.LENGTH_SHORT).show()
                    null
                } else {
                    val compressed: File = ImageUtils.compressImage(
                        context = this,
                        uri = uri,
                        filename = "storeimg",
                        maxWidth = 1024,
                        maxHeight = 1024,
                        quality = 80
                    )
                    FileUtils.createMultipartFromFile("storeimg", compressed)
                }
            } catch (e: Exception) {
                val rawFile = FileUtils.createTempFileFromUri(this, uri)
                rawFile?.let { FileUtils.createMultipartFromFile("storeimg", it) }
            }
        }

        val ktpPart: MultipartBody.Part? = viewModel.ktpUri?.let { uri ->
            val file = FileUtils.createTempFileFromUri(this, uri)
            file?.let { FileUtils.createMultipartFromFile("ktp", it) }
        }

        val npwpPart: MultipartBody.Part? = viewModel.npwpUri?.let { uri ->
            val file = FileUtils.createTempFileFromUri(this, uri)
            file?.let { FileUtils.createMultipartFromFile("npwp", it) }
        }

        val nibPart: MultipartBody.Part? = viewModel.nibUri?.let { uri ->
            val file = FileUtils.createTempFileFromUri(this, uri)
            file?.let { FileUtils.createMultipartFromFile("nib", it) }
        }

        // --- Couriers desired (sync to exactly this set) ---
        val desiredCouriers = viewModel.selectedCouriers.toList()

        // --- (Optional) Payment upsert from UI fields ---
        // If you want to send the bank from the form during re-apply:
        val paymentsToUpsert = buildList {
            val bankName = viewModel.bankName.value
            val bankNum  = viewModel.bankNumber.value?.toString()
            val accName  = viewModel.accountName.value

            if (!bankName.isNullOrBlank() && !bankNum.isNullOrBlank() && !accName.isNullOrBlank()) {
                // If you want to update the first existing payment instead of adding new:
                val existingId = myStoreViewModel.payment.value?.firstOrNull()?.id
                add(
                    PaymentUpdate(
                        id = existingId,            // null => add; id!=null => update
                        bankName = bankName,
                        bankNum = bankNum,
                        accountName = accName,
                        qrisImage = null             // attach File if you have new QRIS to upload
                    )
                )
            }
        }

        // --- Delete list (empty if none) ---
        val paymentIdToDelete = emptyList<Int>()

        // --- Fire the update ---
        myStoreViewModel.updateStoreApproval(
            storeName = nameBody,
            description = descBody,
            storeType = typeBody,
            latitude = latBody,
            longitude = longBody,
            storeProvince = provBody,
            storeCity = cityBody,
            storeSubdistrict = subdistrictBody,
            storeVillage = villageBody,
            storeStreet = streetBody,
            storePostalCode = postalBody,
            storeAddressDetail = detailBody,
            userPhone = userPhoneBody,
            paymentsToUpdate = paymentsToUpsert,
            paymentIdToDelete = paymentIdToDelete,
            storeCourier = desiredCouriers,
            storeImage = storeImgPart,
            ktpImage = ktpPart,
            npwpDocument = npwpPart,
            nibDocument = nibPart
        )

        myStoreViewModel.updateStoreProfileResult.observe(this) {
            Toast.makeText(this, "Pengajuan ulang berhasil dikirim", Toast.LENGTH_SHORT).show()
            finish()
        }
        myStoreViewModel.errorMessage.observe(this) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val TAG = "RegisterStoreActivity"
    }
}