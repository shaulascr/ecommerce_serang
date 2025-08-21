package com.alya.ecommerce_serang.ui.order.review

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.dto.ReviewProductItem
import com.alya.ecommerce_serang.data.api.dto.ReviewUIItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityCreateReviewBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CreateReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateReviewBinding
    private lateinit var sessionManager: SessionManager
    private val reviewItems = mutableListOf<ReviewUIItem>()
    private var addReviewAdapter: AddReviewAdapter? = null

    private val viewModel : CreateReviewViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            CreateReviewViewModel(orderRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateReviewBinding.inflate(layoutInflater)
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

        setupToolbar()
        getIntentData()
        setupRecyclerView()
        observeViewModel()
        setupSubmitButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.btnBack.setOnClickListener { onBackPressed() }
    }

    private fun getIntentData() {
        // First check if multiple items were passed
        val orderItemsJson = intent.getStringExtra("order_items")
        if (orderItemsJson != null) {
            try {
                val type = object : TypeToken<List<ReviewUIItem>>() {}.type
                val items: List<ReviewUIItem> = Gson().fromJson(orderItemsJson, type)

                // Make sure we explicitly set rating and reviewText
                reviewItems.addAll(items.map { item ->
                    ReviewUIItem(
                        orderItemId = item.orderItemId,
                        productName = item.productName,
                        productImage = item.productImage,
                        rating = 5,  // Default to 5 stars
                        reviewText = ""  // Empty by default
                    )
                })
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat ulasan", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // Check if a single item was passed
            val orderItemId = intent.getIntExtra("order_item_id", -1)
            val productName = intent.getStringExtra("product_name") ?: ""
            val productImage = intent.getStringExtra("product_image") ?: ""

            if (orderItemId != -1) {
                reviewItems.add(
                    ReviewUIItem(
                        orderItemId = orderItemId,
                        productName = productName,
                        productImage = productImage,
                        rating = 5,  // Default to 5 stars
                        reviewText = ""  // Empty by default
                    )
                )
            } else {
                Toast.makeText(this, "Tidak ada produk untuk direview", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        addReviewAdapter = AddReviewAdapter(
            reviewItems,
            onRatingChanged = { position, rating ->
                reviewItems[position].rating = rating
            },
            onReviewTextChanged = { position, text ->
                reviewItems[position].reviewText = text
            }
        )

        binding.rvReviewItems.apply {
            layoutManager = LinearLayoutManager(this@CreateReviewActivity)
            adapter = addReviewAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.reviewSubmitStatus.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading indicator
                    // You can add a ProgressBar in your layout and show/hide it here
                }
                is Result.Success -> {
                    // All reviews submitted successfully
                    Toast.makeText(this, "Ulasan berhasil dikirim", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    // Show error message
                    Log.e("CreateReviewActivity", "Error: ${result.exception}")
//                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitReview.setOnClickListener {
            // Validate all reviews
            var isValid = true
            for (item in reviewItems) {
                if (item.reviewText.isBlank()) {
                    isValid = false
                    Toast.makeText(this, "Mohon isi semua ulasan", Toast.LENGTH_SHORT).show()
                    break
                }
            }

            // In setupSubmitButton() method
            if (isValid) {
                viewModel.setTotalReviewsToSubmit(reviewItems.size)

                // Submit all reviews
                for (item in reviewItems) {
                    Log.d("ReviewActivity", "Submitting review for item ${item.orderItemId}: rating=${item.rating}, text=${item.reviewText}")

                    val reviewProductItem = ReviewProductItem(
                        orderItemId = item.orderItemId,
                        rating = item.rating,
                        reviewTxt = item.reviewText
                    )
                    viewModel.submitReview(reviewProductItem)
                }
            }
        }
    }
}