package com.alya.ecommerce_serang.ui.profile.mystore.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.StoreTypesItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailStoreProfileBinding
import com.alya.ecommerce_serang.databinding.DialogStoreImageBinding
import com.alya.ecommerce_serang.ui.profile.mystore.profile.address.DetailStoreAddressActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.payment_info.PaymentInfoActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.shipping_service.ShippingServiceActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.PopUpDialog
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.bumptech.glide.Glide
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class DetailStoreProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoreProfileBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var currentStore: Store? = null
    private var editMode = false
    private var selectedImageUri: Uri? = null
    private var selectedStoreTypeId: Int = -1

    private var storeTypesLoaded: Boolean = false
    private var currentStoreLoaded: Boolean = false
    private var storeTypesList: List<StoreTypesItem> = listOf()

    private val viewModel: MyStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val myStoreRepository = MyStoreRepository(apiService)
            MyStoreViewModel(myStoreRepository)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).into(binding.ivProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoreProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        // Set up header title
        binding.header.headerTitle.text = "Profil Toko"

        // Set up back button
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        viewModel.loadMyStore()
        viewModel.fetchStoreTypes()

        viewModel.myStoreProfile.observe(this) {
            currentStore = it?.store
            currentStoreLoaded = true
            if (storeTypesLoaded) setupStoreTypeSpinner(storeTypesList)
            updateUI(it?.store)
        }

        viewModel.storeTypes.observe(this) {
            storeTypesList = it
            storeTypesLoaded = true
            if (currentStoreLoaded) setupStoreTypeSpinner(storeTypesList)
        }

        binding.ivProfile.setOnClickListener {
            if (editMode) showImageOptions()
        }

        binding.btnEditStoreProfile.setOnClickListener {
            if (editMode) {
                if (hasChanges()) confirmUpdate() else exitEditMode()
            } else {
                enterEditMode()
            }
        }

        binding.layoutAddress.setOnClickListener {
            startActivityForResult(Intent(this, DetailStoreAddressActivity::class.java), 101)
        }

        binding.layoutPaymentMethod.setOnClickListener {
            startActivityForResult(Intent(this, PaymentInfoActivity::class.java), 102)
        }

        binding.layoutShipServices.setOnClickListener {
            startActivityForResult(Intent(this, ShippingServiceActivity::class.java), 103)
        }

        observeViewModel()
    }

    private fun setupStoreTypeSpinner(storeTypes: List<StoreTypesItem>) {
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
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerJenisToko.adapter = adapter

        currentStore?.storeTypeId?.let { typeId ->
            val index = storeTypes.indexOfFirst { it.id == typeId }
            if (index >= 0) binding.spinnerJenisToko.setSelection(index)
        }

        binding.spinnerJenisToko.isEnabled = editMode
        binding.spinnerJenisToko.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = adapter.getItem(pos)
                selected?.let {
                    selectedStoreTypeId = it.id
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.updateStoreProfileResult.observe(this) {
            Toast.makeText(this, "Profil toko berhasil diperbarui", Toast.LENGTH_SHORT).show()
            viewModel.loadMyStore()
            exitEditMode()
        }

        viewModel.errorMessage.observe(this) {
            Log.e("DetailStoreProfileActivity", "Error: $it")
        }
    }

    private fun updateUI(store: Store?) {
        store.let {
            binding.edtNamaToko.setText(it?.storeName)
            binding.edtDeskripsiToko.setText(it?.storeDescription)
            binding.switchIsActive.isChecked = it?.isOnLeave == true

            val imageUrl = when {
                it?.storeImage.toString().isBlank() -> null
                it?.storeImage.toString().startsWith("http") -> it?.storeImage
                it?.storeImage.toString().startsWith("/") -> BASE_URL + it?.storeImage.toString().removePrefix("/")
                else -> BASE_URL + it?.storeImage
            }
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.ivProfile)

            setFieldsEnabled(false)
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.edtNamaToko.isEnabled = enabled
        binding.edtNamaToko.setBackgroundResource(R.drawable.bg_text_field)

        binding.spinnerJenisToko.isEnabled = enabled
        binding.layoutJenisToko.setBackgroundResource(R.drawable.bg_text_field)

        binding.edtDeskripsiToko.isEnabled = enabled
        binding.edtDeskripsiToko.setBackgroundResource(R.drawable.bg_text_field)

        binding.switchIsActive.isEnabled = enabled
    }

    private fun enterEditMode() {
        editMode = true
        setFieldsEnabled(true)
        binding.btnEditStoreProfile.text = "Simpan Perubahan"
        binding.btnEditStoreProfile.setBackgroundResource(R.drawable.bg_button_active)
        binding.btnEditStoreProfile.setTextColor(getColor(R.color.white))
    }

    private fun exitEditMode() {
        editMode = false
        setFieldsEnabled(false)
        binding.btnEditStoreProfile.text = "Ubah Profil"
        binding.btnEditStoreProfile.setBackgroundResource(R.drawable.bg_button_secondary)
        binding.btnEditStoreProfile.setTextColor(getColor(R.color.blue_500))
    }

    private fun hasChanges(): Boolean {
        val nameChanged = binding.edtNamaToko.text.toString() != currentStore?.storeName
        val descChanged = binding.edtDeskripsiToko.text.toString() != currentStore?.storeDescription
        val isOnLeaveChanged = (binding.switchIsActive.isChecked && currentStore?.isOnLeave != false)
                || (!binding.switchIsActive.isChecked && currentStore?.isOnLeave == false)
        val imageChanged = selectedImageUri != null
        val storeTypeChanged = selectedStoreTypeId != currentStore?.storeTypeId
        return nameChanged || descChanged || isOnLeaveChanged || imageChanged || storeTypeChanged
    }

    private fun confirmUpdate() {

        PopUpDialog.showConfirmDialog(
            context = this,
            title = "Apakah Anda yakin ingin menyimpan perubahan profil?",
            message = "Pastikan data yang dimasukkan sudah benar",
            positiveText = "Ya",
            negativeText = "Tidak",
            onYesClicked = {
                updateStoreProfile()
            }
        )
    }

    private fun showImageOptions() {
        val options = arrayOf("Lihat Foto", "Ganti Foto")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showImagePreviewDialog()
                    1 -> imagePickerLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun showImagePreviewDialog() {
        val dialogBinding = DialogStoreImageBinding.inflate(LayoutInflater.from(this))
        val imageUrl = when {
            selectedImageUri != null -> selectedImageUri.toString()
            currentStore?.storeImage.toString().isBlank() -> null
            currentStore?.storeImage.toString().startsWith("http") == true -> currentStore?.storeImage
            currentStore?.storeImage.toString().startsWith("/") == true -> BASE_URL + currentStore?.storeImage!!.toString().removePrefix("/")
            else -> BASE_URL + currentStore?.storeImage
        }
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(dialogBinding.ivPreviewStoreImg)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun updateStoreProfile() {
        val storeName = binding.edtNamaToko.text.toString().toRequestBody()
        val storeType = selectedStoreTypeId.toString().toRequestBody()
        val description = binding.edtDeskripsiToko.text.toString().toRequestBody()
        val isOnLeave = binding.switchIsActive.isChecked.toString().toRequestBody()

        val imagePart = selectedImageUri?.let {
            val file = uriToNamedFile(it, this, "storeimg")
            MultipartBody.Part.createFormData(
                "storeimg", file.name, file.asRequestBody(
                    contentResolver.getType(it)?.toMediaTypeOrNull()
                )
            )
        }

        viewModel.updateStoreProfile(storeName, storeType, description, isOnLeave, imagePart)
    }

    private fun uriToNamedFile(uri: Uri, context: Activity, prefix: String): File {
        val extension = contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val filename = "$prefix-${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, filename)
        contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        return file
    }

    private fun String.toRequestBody(): RequestBody =
        RequestBody.create("text/plain".toMediaTypeOrNull(), this)
}