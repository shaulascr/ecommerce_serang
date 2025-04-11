package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.alya.ecommerce_serang.R
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityStoreProductDetailBinding
import com.alya.ecommerce_serang.utils.viewmodel.ProductViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import kotlin.getValue

class StoreProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreProductDetailBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryList: List<CategoryItem>
    private var imageUri: Uri? = null
    private var sppirtUri: Uri? = null
    private var halalUri: Uri? = null

    private val viewModel: ProductViewModel by viewModels {
        BaseViewModelFactory {
            sessionManager = SessionManager(this)
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            ProductViewModel(productRepository)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            imageUri?.let {
                binding.ivPreviewFoto.setImageURI(it)
                binding.switcherFotoProduk.showNext()
            }
            validateForm()
        }
    }

    private val sppirtLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && isValidFile(uri)) {
            sppirtUri = uri
            binding.tvSppirtName.text = File(uri.path ?: "").name
            binding.switcherSppirt.showNext()
        }
    }

    private val halalLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && isValidFile(uri)) {
            halalUri = uri
            binding.tvHalalName.text = File(uri.path ?: "").name
            binding.switcherHalal.showNext()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()

        // Fetch categories
        viewModel.loadCategories()
        viewModel.categoryList.observe(this) { result ->
            if (result is Result.Success) {
                categoryList = result.data
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerKategoriProduk.adapter = adapter
            }
        }

        // Setup Pre-Order visibility
        binding.switchIsPreOrder.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDurasi.visibility = if (isChecked) View.VISIBLE else View.GONE
            validateForm()
        }

        binding.tvTambahFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        binding.layoutUploadFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        binding.layoutUploadSppirt.setOnClickListener { sppirtLauncher.launch("*/*") }
        binding.btnRemoveSppirt.setOnClickListener {
            sppirtUri = null
            binding.switcherSppirt.showPrevious()
        }

        binding.layoutUploadHalal.setOnClickListener { halalLauncher.launch("*/*") }
        binding.btnRemoveHalal.setOnClickListener {
            halalUri = null
            binding.switcherHalal.showPrevious()
        }

        binding.btnRemoveFoto.setOnClickListener {
            imageUri = null
            binding.switcherFotoProduk.showPrevious()
            validateForm()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }

        listOf(
            binding.edtNamaProduk,
            binding.edtDeskripsiProduk,
            binding.edtHargaProduk,
            binding.edtStokProduk,
            binding.edtMinOrder,
            binding.edtBeratProduk,
            binding.edtDurasi
        ).forEach { it.addTextChangedListener(watcher) }

        validateForm()

        binding.btnSaveProduct.setOnClickListener {
            if (!binding.btnSaveProduct.isEnabled) {
                focusFirstInvalidField()
                return@setOnClickListener
            }
            submitProduct()
        }
    }

    private fun isValidFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri) ?: return false
        return listOf("application/pdf", "image/jpeg", "image/png", "image/jpg").contains(mimeType)
    }

    private fun validateForm() {
        val valid = binding.edtNamaProduk.text.isNotBlank() &&
                binding.edtDeskripsiProduk.text.isNotBlank() &&
                binding.edtHargaProduk.text.isNotBlank() &&
                binding.edtStokProduk.text.isNotBlank() &&
                binding.edtMinOrder.text.isNotBlank() &&
                binding.edtBeratProduk.text.isNotBlank() &&
                (!binding.switchIsPreOrder.isChecked || binding.edtDurasi.text.isNotBlank()) &&
                imageUri != null

        binding.btnSaveProduct.isEnabled = valid
        binding.btnSaveProduct.setTextColor(
            if (valid) ContextCompat.getColor(this, R.color.white) else ContextCompat.getColor(this, R.color.black_300)
        )
        binding.btnSaveProduct.setBackgroundResource(
            if (valid) R.drawable.bg_button_active else R.drawable.bg_button_disabled
        )
    }

    private fun focusFirstInvalidField() {
        when {
            binding.edtNamaProduk.text.isBlank() -> binding.edtNamaProduk.requestFocus()
            binding.edtDeskripsiProduk.text.isBlank() -> binding.edtDeskripsiProduk.requestFocus()
            binding.edtHargaProduk.text.isBlank() -> binding.edtHargaProduk.requestFocus()
            binding.edtStokProduk.text.isBlank() -> binding.edtStokProduk.requestFocus()
            binding.edtMinOrder.text.isBlank() -> binding.edtMinOrder.requestFocus()
            binding.edtBeratProduk.text.isBlank() -> binding.edtBeratProduk.requestFocus()
            binding.switchIsPreOrder.isChecked && binding.edtDurasi.text.isBlank() -> binding.edtDurasi.requestFocus()
            imageUri == null -> Toast.makeText(this, "Silakan unggah foto produk", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload_", ".tmp", context.cacheDir)
        inputStream?.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun submitProduct() {
        val name = binding.edtNamaProduk.text.toString()
        val description = binding.edtDeskripsiProduk.text.toString()
        val price = binding.edtHargaProduk.text.toString().toInt()
        val stock = binding.edtStokProduk.text.toString().toInt()
        val minOrder = binding.edtMinOrder.text.toString().toInt()
        val weight = binding.edtBeratProduk.text.toString().toInt()
        val isPreOrder = binding.switchIsPreOrder.isChecked
        val duration = if (isPreOrder) binding.edtDurasi.text.toString().toInt() else 0
        val isActive = binding.switchIsActive.isChecked
        val categoryId = categoryList.getOrNull(binding.spinnerKategoriProduk.selectedItemPosition)?.id ?: 0

        val imageFile = imageUri?.let { uriToFile(it, this) }
        val sppirtFile = sppirtUri?.let { uriToFile(it, this) }
        val halalFile = halalUri?.let { uriToFile(it, this) }

        viewModel.addProduct(
            name, description, price, stock, minOrder, weight, isPreOrder, duration, categoryId, isActive, imageFile, sppirtFile, halalFile
        ).observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.btnSaveProduct.isEnabled = false
                is Result.Success -> {
                    Toast.makeText(this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Gagal: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSaveProduct.isEnabled = true
                }
            }
        }
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Tambah Produk"
        binding.header.headerLeftIcon.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}