package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityProductBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ProductViewModel

class ProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductBinding
    private lateinit var sessionManager: SessionManager

    private lateinit var productAdapter: ProductAdapter

    private val viewModel: ProductViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            ProductViewModel(productRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)

        super.onCreate(savedInstanceState)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupRecyclerView()
        observeViewModel()

        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadMyStoreProducts()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMyStoreProducts()
    }

    private fun observeViewModel() {
        viewModel.productList.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val products = result.data

                    productAdapter = ProductAdapter(
                        products,
                        onItemClick = { products ->
                            Toast.makeText(this, "Produk ${products.name} diklik", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateProduct = { productId, updatedFields ->
                            viewModel.updateProduct(productId, updatedFields)
                        }
                    )

                    binding.rvStoreProduct.adapter = productAdapter
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Gagal memuat produk: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.productUpdateResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this, "Produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    viewModel.loadMyStoreProducts()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Gagal memperbarui produk", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun setupHeader() {
        binding.headerListProduct.headerTitle.text = "Produk Saya"
        binding.headerListProduct.headerRightText.visibility = View.VISIBLE

        binding.headerListProduct.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.headerListProduct.headerRightText.setOnClickListener {
            val intent = Intent(this, DetailStoreProductActivity::class.java)
            intent.putExtra("is_editing", false)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        binding.rvStoreProduct.layoutManager = LinearLayoutManager(this)
    }


}