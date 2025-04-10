package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
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
import kotlin.getValue

class StoreProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreProductDetailBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryList: List<CategoryItem>

    private val viewModel: ProductViewModel by viewModels {
        BaseViewModelFactory {
            sessionManager = SessionManager(this)
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            ProductViewModel(productRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        observeCategories()
        viewModel.loadCategories()

        // Setup Pre-Order visibility
        binding.switchIsPreOrder.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDurasi.visibility = if (isChecked) View.VISIBLE else View.GONE
            validateForm()
        }

        setupFormValidation()
        validateForm()

        binding.btnSaveProduct.setOnClickListener {
            if (binding.btnSaveProduct.isEnabled) addProduct()
        }
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Tambah Produk"

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeCategories() {
        viewModel.categoryList.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Optionally show loading spinner
                }
                is Result.Success -> {
                    categoryList = result.data
                    setupCategorySpinner(categoryList)
                }
                is Result.Error -> {
                    Toast.makeText(
                        this,
                        "Failed to load categories: ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupCategorySpinner(categories: List<CategoryItem>) {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerKategoriProduk.adapter = adapter
    }

    private fun addProduct() {
        val name = binding.edtNamaProduk.text.toString()
        val description = binding.edtDeskripsiProduk.text.toString()
        val price = binding.edtHargaProduk.text.toString().toIntOrNull() ?: 0
        val stock = binding.edtStokProduk.text.toString().toIntOrNull() ?: 0
        val minOrder = binding.edtMinOrder.text.toString().toIntOrNull() ?: 1
        val weight = binding.edtBeratProduk.text.toString().toIntOrNull() ?: 0
        val isPreOrder = binding.switchIsPreOrder.isChecked
        val duration = binding.edtDurasi.text.toString().toIntOrNull() ?: 0
        val isActive = binding.switchIsActive.isChecked
        val categoryPosition = binding.spinnerKategoriProduk.selectedItemPosition
        val categoryId = categoryList.getOrNull(categoryPosition)?.id ?: 0

        if (isPreOrder && duration == 0) {
            Toast.makeText(this, "Durasi wajib diisi jika pre-order diaktifkan.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.addProduct(
            name, description, price, stock, minOrder, weight, isPreOrder, duration, categoryId, isActive
        ).observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.btnSaveProduct.isEnabled = false
                is Result.Success -> {
                    Toast.makeText(this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    binding.btnSaveProduct.isEnabled = true
                    Toast.makeText(this, "Gagal: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFormValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        // Watch all fields
        binding.edtNamaProduk.addTextChangedListener(watcher)
        binding.edtDeskripsiProduk.addTextChangedListener(watcher)
        binding.edtHargaProduk.addTextChangedListener(watcher)
        binding.edtStokProduk.addTextChangedListener(watcher)
        binding.edtMinOrder.addTextChangedListener(watcher)
        binding.edtBeratProduk.addTextChangedListener(watcher)
        binding.edtDurasi.addTextChangedListener(watcher)
    }

    private fun validateForm() {
        val isNameValid = binding.edtNamaProduk.text.toString().isNotBlank()
        val isDescriptionValid = binding.edtDeskripsiProduk.text.toString().isNotBlank()
        val isPriceValid = binding.edtHargaProduk.text.toString().isNotBlank()
        val isStockValid = binding.edtStokProduk.text.toString().isNotBlank()
        val isMinOrderValid = binding.edtMinOrder.text.toString().isNotBlank()
        val isWeightValid = binding.edtBeratProduk.text.toString().isNotBlank()
        val isPreOrderChecked = binding.switchIsPreOrder.isChecked
        val isDurationValid = !isPreOrderChecked || binding.edtDurasi.text.toString().isNotBlank()

        val isFormValid = isNameValid && isDescriptionValid && isPriceValid &&
                isStockValid && isMinOrderValid && isWeightValid && isDurationValid

        if (isFormValid) {
            binding.btnSaveProduct.isEnabled = true
            binding.btnSaveProduct.setBackgroundResource(R.drawable.bg_button_active)
            binding.btnSaveProduct.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.btnSaveProduct.isEnabled = false
            binding.btnSaveProduct.setBackgroundResource(R.drawable.bg_button_disabled)
            binding.btnSaveProduct.setTextColor(ContextCompat.getColor(this, R.color.black_300))
        }
    }
}