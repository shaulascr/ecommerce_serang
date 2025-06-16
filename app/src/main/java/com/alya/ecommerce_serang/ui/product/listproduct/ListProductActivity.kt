package com.alya.ecommerce_serang.ui.product.listproduct

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityListProductBinding
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.ui.product.ProductUserViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class ListProductActivity : AppCompatActivity() {
    companion object {
        private val TAG = "ListProductActivity"
    }
    private lateinit var binding: ActivityListProductBinding
    private lateinit var sessionManager: SessionManager
    private var productAdapter: ListProductAdapter? = null

    private val viewModel: ProductUserViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val repository = ProductRepository(apiService)
            ProductUserViewModel(repository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

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


        setupObserver()
        setupRecyclerView()
        viewModel.loadProductsList()

    }

    private fun setupRecyclerView() {

        binding.rvProducts.apply {
            adapter = productAdapter
            layoutManager = GridLayoutManager(
                context,
                2,
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        Log.d(TAG, "RecyclerView setup complete with GridLayoutManager")
    }

    private fun setupObserver(){

        viewModel.productList.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    val products = result.data
                    viewModel.loadStoresForProducts(products)
                    Log.d(TAG, "Product list loaded successfully: ${products.size} items")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to load products: ${result.exception.message}")
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                }
            }
        }

        viewModel.storeMap.observe(this){ storeMap ->
            val products = (viewModel.productList.value as? Result.Success)?.data.orEmpty()
            if (products.isNotEmpty()) {
                updateProducts(products, storeMap)
            }
        }

    }

    private fun updateProducts(products: List<ProductsItem>, storeMap: Map<Int, StoreItem>) {
        if (products.isEmpty()) {
            Log.d(TAG, "Product list is empty, hiding RecyclerView")
            binding.rvProducts.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Displaying product list in RecyclerView")
            binding.rvProducts.visibility = View.VISIBLE  // <-- Fix here
            productAdapter = ListProductAdapter(products, onClick = { product ->
                handleProductClick(product)
            }, storeMap = storeMap)
            binding.rvProducts.adapter = productAdapter
            productAdapter?.updateProducts(products)
        }
    }

    private fun handleProductClick(product: ProductsItem) {
        val intent = Intent(this, DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id) // Pass product ID
        startActivity(intent)
    }

}