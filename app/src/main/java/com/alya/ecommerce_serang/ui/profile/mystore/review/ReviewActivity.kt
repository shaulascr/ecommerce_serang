package com.alya.ecommerce_serang.ui.profile.mystore.review

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ReviewRepository
import com.alya.ecommerce_serang.databinding.ActivityReviewBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ReviewViewModel

class ReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: ReviewViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val reviewRepository = ReviewRepository(apiService)
            ReviewViewModel(reviewRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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

        setupHeader()

        viewModel.getReview("all")
        viewModel.averageScore.observe(this) { binding.tvReviewScore.text = it }
        viewModel.totalReview.observe(this) { binding.tvTotalReview.text = "$it rating" }
        viewModel.totalReviewWithDesc.observe(this) { binding.tvTotalReviewWithDesc.text = "$it ulasan" }

        if (savedInstanceState == null) {
            showReviewFragment()
        }
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Ulasan Pembeli"

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }
    }

    private fun showReviewFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container_reviews, ReviewFragment())
        }
    }
}