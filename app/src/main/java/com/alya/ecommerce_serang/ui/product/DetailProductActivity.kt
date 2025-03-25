package com.alya.ecommerce_serang.ui.product

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.Product
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailProductBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide

class DetailProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProductBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private val viewModel: ProductViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            ProductViewModel(productRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        val productId = intent.getIntExtra("PRODUCT_ID", -1)
        if (productId == -1) {
            Log.e("DetailProductActivity", "Invalid Product ID")
            finish() // Close activity if no valid ID
            return
        }

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        viewModel.loadProductDetail(productId)

        viewModel.productDetail.observe(this) { product ->
            if (product != null) {
                Log.d("ProductDetail", "Name: ${product.productName}, Price: ${product.price}")
                // Update UI here, e.g., show in a TextView or ImageView
                viewModel.loadProductDetail(productId)

            } else {
                Log.e("ProductDetail", "Failed to fetch product details")
            }
        }
        observeProductDetail()
    }
    private fun observeProductDetail() {
        viewModel.productDetail.observe(this) { product ->
            product?.let { updateUI(it) }
        }
    }

    private fun updateUI(product: Product){
        binding.tvProductName.text = product.productName
        binding.tvPrice.text = product.price
        binding.tvSold.text = product.totalSold.toString()
        binding.tvRating.text = product.rating
        binding.tvWeight.text = product.weight.toString()
        binding.tvStock.text = product.stock.toString()
        binding.tvCategory.text = product.productCategory
        binding.tvDescription.text = product.description
        binding.tvSellerName.text = product.storeId.toString()

        val fullImageUrl = when (val img = product.image) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image // Default image for null
        }
        Log.d("ProductAdapter", "Loading image: $fullImageUrl")

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(binding.ivProductImage)
    }
}