package com.alya.ecommerce_serang.ui.profile.mystore

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.databinding.ActivityMyStoreBinding
import com.alya.ecommerce_serang.ui.chat.ChatFragment
import com.alya.ecommerce_serang.ui.profile.mystore.balance.BalanceActivity
import com.alya.ecommerce_serang.ui.profile.mystore.product.ProductActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.DetailStoreProfileActivity
import com.alya.ecommerce_serang.ui.profile.mystore.review.ReviewFragment
import com.alya.ecommerce_serang.ui.profile.mystore.sells.all_sells.AllSellsFragment
import com.alya.ecommerce_serang.ui.profile.mystore.sells.order.OrderFragment
import com.alya.ecommerce_serang.ui.profile.mystore.sells.payment.PaymentFragment
import com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment.ShipmentFragment
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import kotlin.getValue

class MyStoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyStoreBinding
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
        binding = ActivityMyStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        viewModel.loadMyStore()

        viewModel.myStoreProfile.observe(this){ user ->
            user?.let { myStoreProfileOverview(it) }
        }

        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        setUpClickListeners()
    }

    private fun myStoreProfileOverview(store: Store){

        binding.tvStoreName.setText(store.storeName.toString())
        binding.tvStoreType.setText(store.storeType.toString())

        store.storeImage.let {
            Glide.with(this)
                .load(it)
                .into(binding.ivProfile)
        }
    }

    private fun setUpClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, DetailStoreProfileActivity::class.java))
        }

        binding.layoutBalance.setOnClickListener {
            startActivity(Intent(this, BalanceActivity::class.java))
        }

        binding.tvHistory.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, AllSellsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.layoutPerluTagihan.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, OrderFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.layoutPembayaran.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PaymentFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.layoutPerluDikirim.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ShipmentFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.layoutProductMenu.setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }

        binding.layoutReview.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ReviewFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.layoutInbox.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ChatFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}