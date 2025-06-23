package com.alya.ecommerce_serang.ui.profile.mystore

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.ActivityStoreOnReviewBinding

class StoreOnReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoreOnReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreOnReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Verifikasi Pengajuan Toko"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }
}