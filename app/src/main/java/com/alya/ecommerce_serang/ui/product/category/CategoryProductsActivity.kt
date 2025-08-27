package com.alya.ecommerce_serang.ui.product.category

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.ActivityCategoryProductsBinding
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class CategoryProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryProductsBinding
    private lateinit var sessionManager: SessionManager
    private var productsAdapter: ProductsCategoryAdapter? = null

    private val viewModel: CategoryProductsViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            CategoryProductsViewModel(productRepository)
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryProductsBinding.inflate(layoutInflater)
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

        val category = intent.getParcelableExtra<CategoryItem>(EXTRA_CATEGORY)
        if (category == null) {
            finish()
            return
        }

        setupUI(category)
        setupRecyclerView()
        observeViewModel()

        // Load products for this category using category.id (not store_type_id)
        viewModel.loadProductsByCategory(category.id)
    }

    private fun setupUI(category: CategoryItem) {
        binding.apply {
            // Setup toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
//                title = category.name
            }

            val fullImageUrl = when (val img = category.image) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> null
            }

            // Load category image
            Glide.with(this@CategoryProductsActivity)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivCategoryHeader)

            tvCategoryTitle.text = category.name
        }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsCategoryAdapter(
            products = emptyList(),
            onClick = { product -> handleProductClick(product) }
        )

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@CategoryProductsActivity, 2)
            adapter = productsAdapter
//            addItemDecoration(GridSpacingItemDecoration(2, 16, true))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CategoryProductsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvProducts.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.layoutEmpty.isVisible = false
                        }
                        is CategoryProductsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false

                            if (state.products.isEmpty()) {
                                binding.rvProducts.isVisible = false
                                binding.layoutEmpty.isVisible = true
                            } else {
                                binding.rvProducts.isVisible = true
                                binding.layoutEmpty.isVisible = false
                                productsAdapter?.updateProducts(state.products)
                            }
                        }
                        is CategoryProductsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.rvProducts.isVisible = false
                            binding.layoutEmpty.isVisible = false
                            binding.layoutError.isVisible = true
                            binding.tvErrorMessage.text = state.message

                            binding.btnRetry.setOnClickListener {
                                val category = intent.getParcelableExtra<CategoryItem>(
                                    EXTRA_CATEGORY
                                )
                                category?.let { viewModel.loadProductsByCategory(it.id) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleProductClick(product: ProductsItem) {
        val intent = Intent(this, DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        productsAdapter = null
    }
}