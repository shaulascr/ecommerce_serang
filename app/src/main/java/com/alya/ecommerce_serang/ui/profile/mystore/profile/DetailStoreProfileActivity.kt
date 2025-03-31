package com.alya.ecommerce_serang.ui.profile.mystore.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailStoreProfileBinding
import com.alya.ecommerce_serang.ui.profile.mystore.MyStoreViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import kotlin.getValue

class DetailStoreProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoreProfileBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private val viewModel: MyStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val myStoreRepository = MyStoreRepository(apiService)
            MyStoreViewModel(myStoreRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoreProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        viewModel.loadMyStore()

        viewModel.myStoreProfile.observe(this){ user ->
            user?.let { updateStoreProfile(it) }
        }

        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStoreProfile(store: Store){

        binding.edtNamaToko.setText(store.storeName.toString())
        binding.edtJenisToko.setText(store.storeType.toString())
        binding.edtDeskripsiToko.setText(store.storeDescription.toString())

    }
}