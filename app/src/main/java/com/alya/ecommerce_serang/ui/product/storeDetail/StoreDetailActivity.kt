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
import androidx.recyclerview.widget.GridLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
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
        loadData()
        setupUI()
        setupObservers()
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
                    binding.progressBarDetailProdItem.visibility = View.GONE
                    updateStoreInfo(result.data)
                    viewModel.loadOtherProducts(result.data.storeId)
                }
                is Result.Error -> {
                    // Show error message, maybe a Toast or Snackbar
                    binding.progressBarDetailProdItem.visibility = View.GONE
                    Toast.makeText(this, "Failed to load store: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                    binding.progressBarDetailProdItem.visibility = View.VISIBLE
                }
            }
        }
        viewModel.otherProducts.observe(this) { products ->
            viewModel.loadStoresForProducts(products)
//            updateOtherProducts(products)
        }

        viewModel.storeMap.observe(this){ storeMap ->
            val products = viewModel.otherProducts.value.orEmpty()
            if (products.isNotEmpty()) {
                updateProducts(products, storeMap)
            } else {
                binding.progressBarDetailProdItem.visibility = View.VISIBLE
                binding.rvProducts.visibility = View.GONE
            }
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

    private fun updateStoreInfo(store: StoreItem?) {
        store?.let {
            binding.tvStoreName.text = it.storeName
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

            val ratingStr = it.storeRating
            val ratingValue = ratingStr?.toFloatOrNull() ?: 0f

            if (ratingValue != null && ratingValue > 0f) {
                binding.tvStoreRating.text = String.format("%.1f", ratingValue)
                binding.tvStoreRating.visibility = View.VISIBLE
            } else {
                binding.tvStoreRating.text = "Belum ada rating"
                binding.tvStoreRating.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
        }
    }

    private fun updateProducts(products: List<ProductsItem>, storeMap: Map<Int, StoreItem>) {
        if (products.isEmpty()) {
            binding.rvProducts.visibility = View.GONE
            binding.progressBarDetailProdItem.visibility = View.VISIBLE
            Log.d("StoreDetailActivity", "Product list is empty, hiding RecyclerView")
        } else {
            Log.d("StoreDetailActivity", "Displaying product list in RecyclerView")

            binding.progressBarDetailProdItem.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
            productAdapter = HorizontalProductAdapter(products, onClick = { product ->
                handleProductClick(product)
            }, storeMap = storeMap)
            binding.rvProducts.adapter = productAdapter
            productAdapter?.updateProducts(products)
        }
    }


    private fun setupRecyclerViewOtherProducts(){

        binding.rvProducts.apply {
            adapter = productAdapter
            layoutManager = GridLayoutManager(context, 2)
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