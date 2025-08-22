package com.alya.ecommerce_serang.ui.product

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.Product
import com.alya.ecommerce_serang.data.api.response.customer.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityDetailProductBinding
import com.alya.ecommerce_serang.ui.cart.CartActivity
import com.alya.ecommerce_serang.ui.chat.ChatActivity
import com.alya.ecommerce_serang.ui.order.CheckoutActivity
import com.alya.ecommerce_serang.ui.product.storeDetail.StoreDetailActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.NumberFormat
import java.util.Locale

class DetailProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProductBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var productAdapter: OtherProductAdapter? = null
    private var reviewsAdapter: ReviewsAdapter? = null
    private var currentQuantity = 1
    private var isWholesaleAvailable: Boolean = false
    private var isWholesaleSelected: Boolean = false
    private var minOrder: Int = 0

    private val viewModel: ProductUserViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            ProductUserViewModel(productRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailProductBinding.inflate(layoutInflater)
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

        viewModel.storeDetail.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    updateStoreInfo(result.data)
                    binding.progressBarDetailStore.visibility = View.GONE
                }
                is Result.Error -> {
                    // Show error message, maybe a Toast or Snackbar
                    binding.progressBarDetailStore.visibility = View.GONE
                    Log.e("DetailProfileActivity", "Failed to load store: ${result.exception.message}")
                    Toast.makeText(this, "Kendala memuat toko", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                    binding.progressBarDetailStore.visibility = View.VISIBLE
                }
            }
        }

        viewModel.otherProducts.observe(this) { products ->
            viewModel.loadStoresForProducts(products)
//            updateOtherProducts(products)
        }

        viewModel.reviewProduct.observe(this) { reviews ->
            setupRecyclerViewReviewsProduct(reviews)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarDetailProd.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.addCart.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    val cartId = result.data.data.cartId
                    Toast.makeText(this, result.data.message, Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Failed to add to cart: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                }
            }
        }

        viewModel.storeMap.observe(this){ storeMap ->
            val products = viewModel.otherProducts.value.orEmpty()
            if (products.isNotEmpty()) {
                updateOtherProducts(products, storeMap)
            } else {
                binding.emptyOtherProducts.visibility = View.VISIBLE
                binding.recyclerViewOtherProducts.visibility = View.GONE
                binding.tvViewAllProducts.visibility = View.GONE
            }
        }
    }

    //info toko
    private fun updateStoreInfo(store: StoreItem?) {
        store?.let {
            binding.tvSellerName.text = it.storeName
            binding.tvSellerRating.text = it.storeRating
            binding.tvSellerLocation.text = it.storeLocation

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
                .into(binding.ivSellerImage)
        }
    }


    private fun updateOtherProducts(products: List<ProductsItem>, storeMap: Map<Int, StoreItem>) {
        if (products.isEmpty()) {
            Log.d("DetailProductActivity", "Product list is empty, hiding RecyclerView")
            binding.recyclerViewOtherProducts.visibility = View.GONE
            binding.emptyOtherProducts.visibility = View.VISIBLE
            binding.tvViewAllProducts.visibility = View.GONE
        } else {
            Log.d("DetailProductActivity", "Displaying product list in RecyclerView")
            binding.recyclerViewOtherProducts.visibility = View.VISIBLE
            binding.tvViewAllProducts.visibility = View.VISIBLE
            binding.emptyOtherProducts.visibility = View.GONE

            productAdapter = OtherProductAdapter(products, onClick = { product ->
                handleProductClick(product)
            }, storeMap = storeMap)
            binding.recyclerViewOtherProducts.adapter = productAdapter
            productAdapter?.updateProducts(products)
        }
    }

    private fun setupUI() {
        binding.searchContainer.btnBack.setOnClickListener {
            finish()
        }

        binding.tvViewAllReviews.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { productId ->
                handleAllReviewsClick(productId)
            }
        }

        val searchContainerView = binding.searchContainer
        searchContainerView.btnCart.setOnClickListener{
            navigateToCart()
        }

        setupRecyclerViewOtherProducts()
    }

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }

    private fun updateUI(product: Product){
        binding.tvProductName.text = product.productName
        binding.tvPrice.text = formatCurrency(product.price.toDouble())
        binding.tvSold.text = "Terjual ${product.totalSold} buah"
        binding.tvWeight.text = "${product.weight} gram"
        binding.tvStock.text = "${product.stock} buah"
        binding.tvCategory.text = product.productCategory
        binding.tvDescription.text = product.description

        minOrder = product.wholesaleMinItem ?: 1
        isWholesaleAvailable = product.isWholesale ?: false
        isWholesaleSelected = false // Default to regular pricing
        if (isWholesaleAvailable) {
            binding.containerWholesale.visibility = View.VISIBLE
            binding.tvPriceWholesale.text = formatCurrency(product.wholesalePrice!!.toDouble())
            binding.descMinOrder.text = "Minimal pembelian ${minOrder}"
        } else {
            binding.containerWholesale.visibility = View.GONE
        }

        binding.btnChat.setOnClickListener{
            navigateToChat()
        }

        binding.btnBuyNow.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { id ->
                showBuyNowPopup(id)
            }
        }

        binding.containerStoreDetail.setOnClickListener{
            handleStoreClick(product)
        }

        binding.btnAddToCart.setOnClickListener {
            viewModel.productDetail.value?.productId?.let { id ->
                showAddToCartPopup(id)
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

        val ratingStr = product.rating
        val ratingValue = ratingStr?.toFloatOrNull()

        if (ratingValue != null && ratingValue > 0f) {
            binding.tvRating.text = String.format("%.1f", ratingValue)
            binding.tvRating.visibility = View.VISIBLE
        } else {
            binding.tvRating.text = "Belum ada rating"
            binding.tvRating.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    private fun handleAllReviewsClick(productId: Int) {
        val intent = Intent(this, ReviewProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", productId) // Pass product ID
        startActivity(intent)
    }

    private fun setupRecyclerViewOtherProducts(){

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
            binding.emptyReview.visibility = View.VISIBLE
            binding.tvViewAllReviews.visibility = View.GONE
//            binding.tvNoReviews.visibility = View.VISIBLE
        } else {
            binding.recyclerViewReviews.visibility = View.VISIBLE
            binding.tvViewAllReviews.visibility = View.VISIBLE
            binding.emptyReview.visibility = View.GONE
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

    private fun handleStoreClick(product: Product) {
        val intent = Intent(this, StoreDetailActivity::class.java)
        intent.putExtra("STORE_ID", product.storeId) // Pass product ID
        startActivity(intent)
    }

    private fun showBuyNowPopup(productId: Int) {
        showQuantityDialog(productId, true)
    }

    private fun showAddToCartPopup(productId: Int) {
        showQuantityDialog(productId, false)

    }

    //dialog tambah quantity dan harga grosir
    private fun showQuantityDialog(productId: Int, isBuyNow: Boolean) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_count_buy, null)
        bottomSheetDialog.setContentView(view)

        val btnDecrease = view.findViewById<ImageButton>(R.id.btnDecrease)
        val btnIncrease = view.findViewById<ImageButton>(R.id.btnIncrease)

        val tvQuantity = view.findViewById<TextView>(R.id.tvQuantity)
        val btnBuyNow = view.findViewById<Button>(R.id.btnBuyNow)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseDialog)

        val switchWholesale = view.findViewById<SwitchCompat>(R.id.switch_price)
        val titleWholesale = view.findViewById<TextView>(R.id.tv_active_wholesale)
//        val descWholesale = view.findViewById<TextView>(R.id.tv_desc_wholesale)

        if (!isBuyNow) {
            btnBuyNow.setText(R.string.add_to_cart)
        }

        switchWholesale.isEnabled = isWholesaleAvailable
        switchWholesale.isChecked = isWholesaleSelected

        currentQuantity = if (isWholesaleSelected) minOrder else 1
        tvQuantity.text = currentQuantity.toString()

        if (isWholesaleAvailable){
            switchWholesale.visibility = View.VISIBLE
            Toast.makeText(this, "Minimal pembelian grosir $currentQuantity produk", Toast.LENGTH_SHORT).show()
        } else {
            titleWholesale.visibility = View.GONE
            switchWholesale.visibility = View.GONE
        }

        switchWholesale.setOnCheckedChangeListener { _, isChecked ->
            isWholesaleSelected = isChecked

            // Reset quantity when switching between retail and wholesale
            if (isChecked) {
                currentQuantity = minOrder
            } else {
                currentQuantity = 1
            }
            tvQuantity.text = currentQuantity.toString()
        }

        val maxStock = viewModel.productDetail.value?.stock ?: 1

        btnDecrease.setOnClickListener {
            if (isWholesaleSelected) {
                if (currentQuantity > minOrder) {
                    currentQuantity--
                    tvQuantity.text = currentQuantity.toString()
                } else {
                    Toast.makeText(this, "Sudah mencapai jumlah minimum", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (currentQuantity > 1) {
                    currentQuantity--
                    tvQuantity.text = currentQuantity.toString()
                }
            }
        }

        btnIncrease.setOnClickListener {
            if (currentQuantity < maxStock) {
                currentQuantity++
                tvQuantity.text = currentQuantity.toString()
            } else {
                Toast.makeText(this, "Sudah mencapai jumlah maksimum", Toast.LENGTH_SHORT).show()
            }
        }

        btnBuyNow.setOnClickListener {
            bottomSheetDialog.dismiss()

            if (isBuyNow) {
                navigateToCheckout()
            } else {
                val cartItem = CartItem(
                    productId = productId,
                    quantity = currentQuantity
                )
                viewModel.reqCart(cartItem)
            }
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    private fun navigateToCheckout() {
        val productDetail = viewModel.productDetail.value ?: return
        val storeDetail = viewModel.storeDetail.value

        if (storeDetail !is Result.Success || storeDetail.data == null) {
            Toast.makeText(this, "Store information not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (isWholesaleSelected) {
            // Start checkout activity with buy now flow
            //checkout klo grosiran
            CheckoutActivity.startForBuyNow(
                context = this,
                storeId = productDetail.storeId,
                storeName = storeDetail.data.storeName,
                productId = productDetail.productId,
                productName = productDetail.productName,
                productImage = productDetail.image,
                quantity = currentQuantity,
                price = productDetail.wholesalePrice!!.toDouble(),
                isWholesale = true
            )
        } else {
            //checkout klo direct buy normal price
            CheckoutActivity.startForBuyNow(
                context = this,
                storeId = productDetail.storeId,
                storeName = storeDetail.data.storeName,
                productId = productDetail.productId,
                productName = productDetail.productName,
                productImage = productDetail.image,
                quantity = currentQuantity,
                price = productDetail.price.toDouble(),
                isWholesale = false
            )
        }
    }

    private fun navigateToChat(){
        val productDetail = viewModel.productDetail.value ?: return
        val storeDetail = viewModel.storeDetail.value

        if (storeDetail !is Result.Success || storeDetail.data == null) {
            Toast.makeText(this, "Store information not available", Toast.LENGTH_SHORT).show()
            return
        }
        ChatActivity.createIntent(
            context = this,
            storeId = productDetail.storeId,
            productId = productDetail.productId,
            productName = productDetail.productName,
            productPrice = productDetail.price,
            productImage = productDetail.image,
            productRating = productDetail.rating,
            storeName = storeDetail.data.storeName,
            chatRoomId = 0,
            storeImage = storeDetail.data.storeImage,
            attachProduct = true // This will auto-attach the product!

        )
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    companion object {
        private const val EXTRA_PRODUCT_ID = "extra_product_id"

        fun start(context: Context, productId: Int) {
            val intent = Intent(context, DetailProductActivity::class.java)
            intent.putExtra(EXTRA_PRODUCT_ID, productId)
            context.startActivity(intent)
        }
    }
}