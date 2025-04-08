package com.alya.ecommerce_serang.ui.product

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.product.Product
import com.alya.ecommerce_serang.data.api.response.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailProductBinding
import com.alya.ecommerce_serang.ui.home.HorizontalProductAdapter
import com.alya.ecommerce_serang.ui.order.CheckoutActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog

class DetailProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProductBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var productAdapter: HorizontalProductAdapter? = null
    private var reviewsAdapter: ReviewsAdapter? = null

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
        //nanti tambah get store id dari HomeFragment Product.storeId
        if (productId == -1) {
            Log.e("DetailProductActivity", "Invalid Product ID")
            finish() // Close activity if no valid ID
            return
        }

        viewModel.loadProductDetail(productId)
        viewModel.loadReviews(productId)

        observeProductDetail()
        observeProductReviews()
    }

    private fun observeProductDetail() {
        viewModel.productDetail.observe(this) { product ->
            product?.let {
                updateUI(it)
                viewModel.loadOtherProducts(it.storeId)
            }
        }
        viewModel.otherProducts.observe(this) { products ->
            updateOtherProducts(products)
        }
    }

    private fun observeProductReviews() {
        viewModel.reviewProduct.observe(this) { reviews ->
            setupRecyclerViewReviewsProduct(reviews)
        }
    }

    private fun updateOtherProducts(products: List<ProductsItem>) {
        productAdapter?.updateProducts(products) // Make sure your adapter has a method to update data
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

        binding.tvViewAllReviews.setOnClickListener{
            handleAllReviewsClick(product.productId)
        }

        binding.btnBuyNow.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { id ->
                showBuyNowPopup(id)
            }
        }


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

        setupRecyclerViewOtherProducts()
    }

    private fun handleAllReviewsClick(productId: Int) {
        val intent = Intent(this, ReviewProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", productId) // Pass product ID
        startActivity(intent)
    }

    private fun setupRecyclerViewOtherProducts(){
        productAdapter = HorizontalProductAdapter(
            products = emptyList(),
            onClick = { productsItem ->  handleProductClick(productsItem) }
        )

        binding.recyclerViewOtherProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupRecyclerViewReviewsProduct(reviewList: List<ReviewsItem>){
        val limitedReviewList = if (reviewList.isNotEmpty()) listOf(reviewList.first()) else emptyList()

        reviewsAdapter = ReviewsAdapter(
            reviewList = limitedReviewList
        )

        binding.recyclerViewReviews.apply {
            adapter = reviewsAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
        }
    }

    private fun handleProductClick(product: ProductsItem) {
        val intent = Intent(this, DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id) // Pass product ID
        startActivity(intent)
    }

    private fun showBuyNowPopup(productId: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_count_buy, null)
        bottomSheetDialog.setContentView(view)

        val btnDecrease = view.findViewById<Button>(R.id.btnDecrease)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncrease)
        val tvQuantity = view.findViewById<TextView>(R.id.tvQuantity)
        val btnBuyNow = view.findViewById<Button>(R.id.btnBuyNow)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseDialog)

        var quantity = 1
        tvQuantity.text = quantity.toString()

        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }

        btnIncrease.setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
        }

        btnBuyNow.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("PRODUCT_ID", productId)
            intent.putExtra("QUANTITY", quantity)
            startActivity(intent)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

}