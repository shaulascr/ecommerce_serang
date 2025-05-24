package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.alya.ecommerce_serang.data.api.dto.Preorder
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityDetailStoreProductBinding
import com.alya.ecommerce_serang.utils.viewmodel.ProductViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.getValue
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.data.api.dto.Wholesale

class DetailStoreProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailStoreProductBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryList: List<CategoryItem>
    private var imageUri: Uri? = null
    private var sppirtUri: Uri? = null
    private var halalUri: Uri? = null
    private var productId: Int? = null
    private var hasImage: Boolean = false

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
                hasImage = true
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
        binding = ActivityDetailStoreProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isEditing = intent.getBooleanExtra("is_editing", false)
        productId = intent.getIntExtra("product_id", -1)

        binding.header.headerTitle.text = if (isEditing) "Ubah Produk" else "Tambah Produk"

        if (isEditing && productId != null && productId != -1) {
            viewModel.loadProductDetail(productId!!)
            viewModel.productDetail.observe(this) { product ->
                product?.let { populateForm(it) }
            }
        }

        setupCategorySpinner()
        setupFilePickers()

        var conditionList = listOf("Baru", "Pernah Dipakai")
        val adapterCondition = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditionList)
        adapterCondition.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerKondisiProduk.adapter = adapterCondition

        // Setup Pre-Order visibility
        binding.switchIsPreOrder.setOnCheckedChangeListener { _, isChecked ->
            togglePreOrderVisibility(isChecked)
            validateForm()
        }

        binding.switchIsWholesale.setOnCheckedChangeListener { _, isChecked ->
            toggleWholesaleVisibility(isChecked)
            validateForm()
        }

        with(binding) {
            edtNamaProduk.doAfterTextChanged { validateForm() }
            edtDeskripsiProduk.doAfterTextChanged { validateForm() }
            edtHargaProduk.doAfterTextChanged { validateForm() }
            edtStokProduk.doAfterTextChanged { validateForm() }
            edtMinOrder.doAfterTextChanged { validateForm() }
            edtBeratProduk.doAfterTextChanged { validateForm() }
            edtDurasi.doAfterTextChanged { validateForm() }
            edtMinPesanGrosir.doAfterTextChanged { validateForm() }
            edtHargaGrosir.doAfterTextChanged { validateForm() }
            switchIsPreOrder.setOnCheckedChangeListener { _, _ -> validateForm() }
            switchIsWholesale.setOnCheckedChangeListener { _, _ -> validateForm() }
        }

        validateForm()

        binding.btnSaveProduct.setOnClickListener {
            if (isEditing) {
                updateProduct(productId)
            } else {
                addProduct()
            }
        }

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCategorySpinner() {
        viewModel.loadCategories()
        viewModel.categoryList.observe(this) { result ->
            if (result is Result.Success) {
                categoryList = result.data
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerKategoriProduk.adapter = adapter
            }
        }
    }

    private fun setupFilePickers() {
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
            hasImage = false
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
    }

    private fun populateForm(product: com.alya.ecommerce_serang.data.api.response.customer.product.Product) {
        binding.edtNamaProduk.setText(product.productName)
        binding.edtDeskripsiProduk.setText(product.description)
        binding.edtHargaProduk.setText(product.price.toString())
        binding.edtStokProduk.setText(product.stock.toString())
        binding.edtMinOrder.setText(product.minOrder.toString())
        binding.edtBeratProduk.setText(product.weight.toString())
        binding.spinnerKondisiProduk.setSelection(if (product.condition == "Baru") 0 else 1)

        // Category selection
        product.categoryId.let { categoryId ->
            val index = categoryList.indexOfFirst { it.id == categoryId }
            if (index != -1) binding.spinnerKategoriProduk.setSelection(index)
        }

        // Pre-order
        val isPreOrder = product.isPreOrder == true
        binding.switchIsPreOrder.jumpDrawablesToCurrentState()
        binding.switchIsPreOrder.isChecked = isPreOrder
        togglePreOrderVisibility(isPreOrder)
        if (isPreOrder) {
            binding.edtDurasi.setText(product.preorderDuration?.toString() ?: "")
        }

        // Wholesale
        val isWholesale = product.isWholesale == true
        binding.switchIsWholesale.jumpDrawablesToCurrentState()
        binding.switchIsWholesale.isChecked = isWholesale
        toggleWholesaleVisibility(isWholesale)
        if (isWholesale) {
            binding.edtMinPesanGrosir.setText(product.wholesaleMinItem?.toString() ?: "")
            binding.edtHargaGrosir.setText(product.wholesalePrice?.toString() ?: "")
        }

        // Product image
        val imageUrl = if (product.image.startsWith("/")) {
            BASE_URL + product.image.removePrefix("/")
        } else product.image
        Glide.with(this).load(imageUrl).into(binding.ivPreviewFoto)
        binding.switcherFotoProduk.showNext()
        hasImage = true

        // SPPIRT
        product.sppirt?.let {
            binding.tvSppirtName.text = getFileName(it.toUri())
            binding.switcherSppirt.showNext()
        }

        // Halal
        product.halal?.let {
            binding.tvHalalName.text = getFileName(it.toUri())
            binding.switcherHalal.showNext()
        }

        validateForm()
    }

    private fun togglePreOrderVisibility(isChecked: Boolean) {
        Log.d("DEBUG", "togglePreOrderVisibility: $isChecked")
        binding.layoutDurasi.visibility = if (isChecked) View.VISIBLE else View.GONE
    }

    private fun toggleWholesaleVisibility(isChecked: Boolean) {
        Log.d("DEBUG", "toggleWholesaleVisibility: $isChecked")
        binding.layoutMinPesanGrosir.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.layoutHargaGrosir.visibility = if (isChecked) View.VISIBLE else View.GONE
    }

    private fun isValidFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri) ?: return false
        return listOf("application/pdf", "image/jpeg", "image/png", "image/jpg").contains(mimeType)
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown_file"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndexOpenableColumnsDisplayName()
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun Cursor.getColumnIndexOpenableColumnsDisplayName(): Int {
        return try {
            getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
        } catch (e: Exception) {
            -1
        }
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

    fun getMimeType(file: File): String {
        val extension = file.extension
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    fun createPartFromFile(field: String, file: File?): MultipartBody.Part? {
        return file?.let {
            val mimeType = getMimeType(it).toMediaTypeOrNull()
            val requestBody = RequestBody.create(mimeType, it)
            MultipartBody.Part.createFormData(field, it.name, requestBody)
        }
    }

    private fun validateForm() {
        val name = binding.edtNamaProduk.text.toString().trim()
        val description = binding.edtDeskripsiProduk.text.toString().trim()
        val price = binding.edtHargaProduk.text.toString().trim()
        val stock = binding.edtStokProduk.text.toString().trim()
        val minOrder = binding.edtMinOrder.text.toString().trim()
        val weight = binding.edtBeratProduk.text.toString().trim()
        val duration = binding.edtDurasi.text.toString().trim()
        val wholesaleMinItem = binding.edtMinPesanGrosir.text.toString().trim()
        val wholesalePrice = binding.edtHargaGrosir.text.toString().trim()
        val category = binding.spinnerKategoriProduk.selectedItemPosition != -1
        val isPreOrderChecked = binding.switchIsPreOrder.isChecked
        val isWholesaleChecked = binding.switchIsWholesale.isChecked

        val valid = name.isNotEmpty() &&
                description.isNotEmpty() &&
                price.isNotEmpty() &&
                stock.isNotEmpty() &&
                minOrder.isNotEmpty() &&
                weight.isNotEmpty() &&
                (!isPreOrderChecked || duration.isNotEmpty()) &&
                (!isWholesaleChecked || (wholesaleMinItem.isNotEmpty() && wholesalePrice.isNotEmpty())) &&
                category &&
                hasImage

        binding.btnSaveProduct.isEnabled = valid
        binding.btnSaveProduct.setTextColor(
            if (valid) ContextCompat.getColor(this, R.color.white)
            else ContextCompat.getColor(this, R.color.black_300)
        )
        binding.btnSaveProduct.setBackgroundResource(
            if (valid) R.drawable.bg_button_active
            else R.drawable.bg_button_disabled
        )
    }

    private fun addProduct() {
        val name = binding.edtNamaProduk.text.toString().trim()
        val description = binding.edtDeskripsiProduk.text.toString().trim()
        val price = binding.edtHargaProduk.text.toString().toIntOrNull() ?: return showInputError("Harga produk")
        val stock = binding.edtStokProduk.text.toString().toIntOrNull() ?: return showInputError("Stok produk")
        val minOrder = binding.edtMinOrder.text.toString().toIntOrNull() ?: return showInputError("Minimal order")
        val weight = binding.edtBeratProduk.text.toString().toIntOrNull() ?: return showInputError("Berat produk")

        val isPreOrder = binding.switchIsPreOrder.isChecked
        val duration = if (isPreOrder) {
            binding.edtDurasi.text.toString().toIntOrNull() ?: return showInputError("Durasi pre-order")
        } else 0

        val isWholesale = binding.switchIsWholesale.isChecked
        val minOrderWholesale = if (isWholesale) {
            binding.edtMinPesanGrosir.text.toString().toIntOrNull() ?: return showInputError("Min. grosir")
        } else 0

        val priceWholesale = if (isWholesale) {
            binding.edtHargaGrosir.text.toString().toIntOrNull() ?: return showInputError("Harga grosir")
        } else 0

        val status = if (binding.switchIsActive.isChecked) "active" else "inactive"
        val condition = binding.spinnerKondisiProduk.selectedItem.toString()
        val categoryId = categoryList.getOrNull(binding.spinnerKategoriProduk.selectedItemPosition)?.id ?: 0

        val imageFile = imageUri?.let { uriToNamedFile(it, this, "productimg") }
        val sppirtFile = sppirtUri?.let { uriToNamedFile(it, this, "sppirt") }
        val halalFile = halalUri?.let { uriToNamedFile(it, this, "halal") }

        Log.d("File URI", "SPPIRT URI: ${sppirtUri.toString()}")
        Log.d("File URI", "Halal URI: ${halalUri.toString()}")

        val imagePart = imageFile?.let { createPartFromFile("productimg", it) }
        val sppirtPart = sppirtFile?.let { createPartFromFile("sppirt", it) }
        val halalPart = halalFile?.let { createPartFromFile("halal", it) }

        val preorder = Preorder(productId = productId, duration = duration)
        val wholesale = Wholesale(productId = productId, minItem = minOrderWholesale, wholesalePrice = priceWholesale.toString())

        viewModel.addProduct(
            name, description, price, stock, minOrder, weight,
            isPreOrder, preorder,
            isWholesale, wholesale,
            categoryId, status, condition,
            imagePart, sppirtPart, halalPart
        )

        viewModel.productCreationResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.btnSaveProduct.isEnabled = false
                is Result.Success -> {
                    val product = result.data.product
                    Toast.makeText(this, "Product Created: ${product?.name}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    Log.e("ProductDetailActivity", "Error: ${result.exception.message}")
                    binding.btnSaveProduct.isEnabled = true
                }
            }
        }
    }

    private fun showInputError(fieldName: String): Nothing {
        Toast.makeText(this, "$fieldName tidak boleh kosong dan harus berupa angka.", Toast.LENGTH_SHORT).show()
        return throw IllegalArgumentException("$fieldName invalid")
    }

    private fun updateProduct(productId: Int?) {
        val updatedProduct = mapOf(
            "name" to binding.edtNamaProduk.text.toString(),
            "description" to binding.edtDeskripsiProduk.text.toString(),
            "price" to binding.edtHargaProduk.text.toString(),
            "stock" to binding.edtStokProduk.text.toString().toInt(),
            "min_order" to binding.edtMinOrder.text.toString().toInt(),
            "weight" to binding.edtBeratProduk.text.toString().toInt(),
            "is_pre_order" to binding.switchIsPreOrder.isChecked,
            "duration" to (binding.edtDurasi.text.toString().toIntOrNull() ?: 0),
            "is_wholesale" to binding.switchIsWholesale.isChecked,
            "wholesale_min_item" to (binding.edtMinPesanGrosir.text.toString().toIntOrNull() ?: 0),
            "wholesale_price" to (binding.edtHargaGrosir.text.toString().toIntOrNull() ?: 0),
            "category_id" to categoryList[binding.spinnerKategoriProduk.selectedItemPosition].id,
            "status" to if (binding.switchIsActive.isChecked) "active" else "inactive",
            "condition" to binding.spinnerKondisiProduk.selectedItem.toString(),
            "productimg" to imageUri?.path,
            "sppirt" to sppirtUri?.path,
            "halal" to halalUri?.path
        )

        viewModel.updateProduct(productId, updatedProduct)
        viewModel.productUpdateResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.btnSaveProduct.isEnabled = false
                is Result.Success -> {
                    val product = result.data.product
                    Toast.makeText(this, "Produk berhasil diubah: ${product?.name}", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    Log.e("ProductDetailActivity", "Error: ${result.exception.message}")
                    binding.btnSaveProduct.isEnabled = true
                }
            }
        }
    }
}