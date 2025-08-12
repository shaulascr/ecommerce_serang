package com.alya.ecommerce_serang.ui.profile.mystore

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.ActivityStoreSuspendedBinding

class StoreSuspendedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreSuspendedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreSuspendedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Toko Dinonaktifkan"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }
}