package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
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
            binding.tvSppirtName.text = getFileName(uri)
            binding.switcherSppirt.showNext()
        }
    }

    private val halalLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && isValidFile(uri)) {
            halalUri = uri
            binding.tvHalalName.text = getFileName(uri)
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

        binding.btnRemoveFoto.setOnClickListener {
            imageUri = null
            binding.switcherFotoProduk.showPrevious()
            validateForm()
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

        validateForm()

        binding.btnSaveProduct.setOnClickListener {
            if (!binding.btnSaveProduct.isEnabled) {
                return@setOnClickListener
            }
            submitProduct()
        }
    }

    private fun isValidFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri) ?: return false
        return listOf("application/pdf", "image/jpeg", "image/png", "image/jpg").contains(mimeType)
    }

    private fun getFileName(uri: Uri): String {
        return uri.lastPathSegment?.split("/")?.last() ?: "unknown_file"
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

    private fun uriToNamedFile(uri: Uri, context: Context, prefix: String): File {
        val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val filename = "$prefix-${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, filename)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }

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
        val status = if (binding.switchIsActive.isChecked) "active" else "inactive"
        val categoryId = categoryList.getOrNull(binding.spinnerKategoriProduk.selectedItemPosition)?.id ?: 0

        val imageFile = imageUri?.let { File(it.path) }
        val sppirtFile = sppirtUri?.let { uriToNamedFile(it, this, "sppirt") }
        val halalFile = halalUri?.let { uriToNamedFile(it, this, "halal") }

        Log.d("File URI", "SPPIRT URI: ${sppirtUri.toString()}")
        Log.d("File URI", "Halal URI: ${halalUri.toString()}")

        val imagePart = imageFile?.let { createPartFromFile("image", it) }
        val sppirtPart = sppirtFile?.let { createPartFromFile("sppirt", it) }
        val halalPart = halalFile?.let { createPartFromFile("halal", it) }

        viewModel.addProduct(
            name, description, price, stock, minOrder, weight, isPreOrder, duration, categoryId, status, imagePart, sppirtPart, halalPart
        )

        viewModel.productCreationResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.btnSaveProduct.isEnabled = false
                is Result.Success -> {
                    val product = result.data.product
                    Toast.makeText(this, "Product Created: ${product?.productName}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    Log.e("ProductDetailActivity", "Error: ${result.exception.message}")
                    binding.btnSaveProduct.isEnabled = true
                }
            }
        }
    }

    fun createPartFromFile(field: String, file: File?): MultipartBody.Part? {
        return file?.let {
            val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), it)
            MultipartBody.Part.createFormData(field, it.name, requestBody)
        }
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Tambah Produk"
        binding.header.headerLeftIcon.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}