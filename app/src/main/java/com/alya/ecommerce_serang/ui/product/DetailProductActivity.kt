package com.alya.ecommerce_serang.ui.product

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.product.Product
import com.alya.ecommerce_serang.data.api.response.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.response.product.StoreProduct
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
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
    private var currentQuantity = 1


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

//        val productId = intent.getIntExtra("PRODUCT_ID", -1)
//        //nanti tambah get store id dari HomeFragment Product.storeId
//        if (productId == -1) {
//            Log.e("DetailProductActivity", "Invalid Product ID")
//            finish() // Close activity if no valid ID
//            return
//        }

        setupUI()
        setupObservers()
        loadData()
    }

    private fun loadData() {
        val productId = intent.getIntExtra("PRODUCT_ID", -1)
        if (productId == -1) {
            Log.e("DetailProductActivity", "Invalid Product ID")
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show()
            finish() // Close activity if no valid ID
            return
        }

        viewModel.loadProductDetail(productId)
        viewModel.loadReviews(productId)
    }

    private fun setupObservers() {
        viewModel.productDetail.observe(this) { product ->
            product?.let {
                updateUI(it)
                viewModel.loadOtherProducts(it.storeId)
            }
        }

        viewModel.storeDetail.observe(this) { store ->
            updateStoreInfo(store)
        }

        viewModel.otherProducts.observe(this) { products ->
            updateOtherProducts(products)
        }

        viewModel.reviewProduct.observe(this) { reviews ->
            setupRecyclerViewReviewsProduct(reviews)
        }
//
//        viewModel.isLoading.observe(this) { isLoading ->
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//
//        viewModel.error.observe(this) { errorMessage ->
//            if (errorMessage.isNotEmpty()) {
//                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
//            }
//        }

        viewModel.addCart.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    Toast.makeText(this, result.data, Toast.LENGTH_SHORT).show()

                    // Check if we need to navigate to checkout (for "Buy Now" flow)
                    if (viewModel.shouldNavigateToCheckout) {
                        viewModel.shouldNavigateToCheckout = false
                        navigateToCheckout()
                    }
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    Toast.makeText(this, "Failed to add to cart: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                }
            }
        }
    }

    private fun updateStoreInfo(store: StoreProduct?) {
        store?.let {
            binding.tvSellerName.text = it.storeName
            // Add more store details as needed
        }
    }

    private fun updateOtherProducts(products: List<ProductsItem>) {
        if (products.isEmpty()) {
            binding.recyclerViewOtherProducts.visibility = View.GONE
            binding.tvViewAllProducts.visibility = View.GONE
        } else {
            binding.recyclerViewOtherProducts.visibility = View.VISIBLE
            binding.tvViewAllProducts.visibility = View.VISIBLE
            productAdapter?.updateProducts(products)
        }    }

    private fun setupUI() {
//        binding.btnBack.setOnClickListener {
//            finish()
//        }

        binding.tvViewAllReviews.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { productId ->
                handleAllReviewsClick(productId)
            }
        }

        binding.btnBuyNow.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { id ->
                showBuyNowPopup(id)
            }
        }

        binding.btnAddToCart.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { id ->
                showAddToCartPopup(id)
            }
        }

        setupRecyclerViewOtherProducts()
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

    private fun handleAllReviewsClick(productId: Int) {
        val intent = Intent(this, ReviewProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", productId) // Pass product ID
        startActivity(intent)
    }

    private fun setupRecyclerViewOtherProducts(){
        productAdapter = HorizontalProductAdapter(
            products = emptyList(),
            onClick = { productsItem -> handleProductClick(productsItem) }
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
        if (reviewList.isEmpty()) {
            binding.recyclerViewReviews.visibility = View.GONE
            binding.tvViewAllReviews.visibility = View.GONE
//            binding.tvNoReviews.visibility = View.VISIBLE
        } else {
            binding.recyclerViewReviews.visibility = View.VISIBLE
            binding.tvViewAllReviews.visibility = View.VISIBLE
        }
//            binding.tvNoReviews.visibility = View.GONE
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
        showQuantityDialog(productId, true)
    }

    private fun showAddToCartPopup(productId: Int) {
        showQuantityDialog(productId, false)
    }

    private fun showQuantityDialog(productId: Int, isBuyNow: Boolean) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_count_buy, null)
        bottomSheetDialog.setContentView(view)

        val btnDecrease = view.findViewById<Button>(R.id.btnDecrease)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncrease)
        val tvQuantity = view.findViewById<TextView>(R.id.tvQuantity)
        val btnBuyNow = view.findViewById<Button>(R.id.btnBuyNow)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseDialog)

        // Set button text based on action
        if (!isBuyNow) {
            btnBuyNow.setText(R.string.add_to_cart)
        }

        currentQuantity = 1
        tvQuantity.text = currentQuantity.toString()

        val maxStock = viewModel.productDetail.value?.stock ?: 1

        btnDecrease.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                tvQuantity.text = currentQuantity.toString()
            }
        }

        btnIncrease.setOnClickListener {
            if (currentQuantity < maxStock) {
                currentQuantity++
                tvQuantity.text = currentQuantity.toString()
            } else {
                Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show()
            }
        }


        btnBuyNow.setOnClickListener {
            bottomSheetDialog.dismiss()

            val cartItem = CartItem(
                productId = productId,
                quantity = currentQuantity
            )

            // For both Buy Now and Add to Cart, we add to cart first
            if (isBuyNow) {
                // Set flag to navigate to checkout after adding to cart is successful
                viewModel.shouldNavigateToCheckout = true
            }

            // Add to cart in both cases
            viewModel.reqCart(cartItem)

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
        }
    }

    private fun navigateToCheckout() {
        val intent = Intent(this, CheckoutActivity::class.java)
        startActivity(intent)
    }
}