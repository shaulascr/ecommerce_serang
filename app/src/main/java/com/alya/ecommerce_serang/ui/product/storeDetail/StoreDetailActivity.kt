package com.alya.ecommerce_serang.ui.product.storeDetail

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreProduct
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityStoreDetailBinding
import com.alya.ecommerce_serang.ui.cart.CartActivity
import com.alya.ecommerce_serang.ui.home.HorizontalProductAdapter
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide

class StoreDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoreDetailBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var productAdapter: HorizontalProductAdapter? = null

    private val viewModel: StoreDetailViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            StoreDetailViewModel(productRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreDetailBinding.inflate(layoutInflater)
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

        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        binding.searchContainer.btnBack.setOnClickListener {
            finish()
        }

        val searchContainerView = binding.searchContainer
        searchContainerView.btnCart.setOnClickListener{
            navigateToCart()
        }

        setupRecyclerViewOtherProducts()
    }

    private fun setupObservers(){

        viewModel.storeDetail.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    updateStoreInfo(result.data)
                    viewModel.loadOtherProducts(result.data.storeId)
                }
                is Result.Error -> {
                    // Show error message, maybe a Toast or Snackbar
                    Toast.makeText(this, "Failed to load store: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                }
            }
        }
        viewModel.otherProducts.observe(this) { products ->
            updateOtherProducts(products)
        }
    }

    private fun loadData() {
        val storeId = intent.getIntExtra("STORE_ID", -1)
        if (storeId == -1) {
            Log.e("StoreDetailActivity", "Invalid store ID")
            Toast.makeText(this, "Invalid store ID", Toast.LENGTH_SHORT).show()
            finish() // Close activity if no valid ID
            return
        }

        viewModel.loadStoreDetail(storeId)
    }

    private fun updateStoreInfo(store: StoreProduct?) {
        store?.let {
            binding.tvStoreName.text = it.storeName
            binding.tvStoreRating.text = it.storeRating
            binding.tvStoreLocation.text = it.storeLocation
            binding.tvStoreType.text = it.storeType
            binding.tvActiveStatus.text = it.status

            // Load store image using Glide
            val fullImageUrl = when (val img = it.storeImage) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> R.drawable.placeholder_image
            }

            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.ivStoreImage)
        }
    }

    private fun updateOtherProducts(products: List<ProductsItem>) {
        if (products.isEmpty()) {
            binding.rvProducts.visibility = View.GONE
        } else {
            binding.rvProducts.visibility = View.VISIBLE
            productAdapter?.updateProducts(products)
        }
    }

    private fun setupRecyclerViewOtherProducts(){
        productAdapter = HorizontalProductAdapter(
            products = emptyList(),
            onClick = { productsItem -> handleProductClick(productsItem) }
        )

        binding.rvProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun handleProductClick(product: ProductsItem) {
        val intent = Intent(this, DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id) // Pass product ID
        startActivity(intent)
    }

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }
}