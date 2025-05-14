package com.alya.ecommerce_serang.ui.profile.mystore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.databinding.ActivityMyStoreBinding
import com.alya.ecommerce_serang.ui.chat.ChatListFragment
import com.alya.ecommerce_serang.ui.profile.mystore.balance.BalanceActivity
import com.alya.ecommerce_serang.ui.profile.mystore.product.ProductActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.DetailStoreProfileActivity
import com.alya.ecommerce_serang.ui.profile.mystore.review.ReviewFragment
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.bumptech.glide.Glide

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

        binding.tvStoreName.text = store.storeName
        binding.tvStoreType.text = store.storeType

        if (store.storeImage != null && store.storeImage.toString().isNotEmpty() && store.storeImage.toString() != "null") {
            val imageUrl = "$BASE_URL${store.storeImage}"
            Log.d("MyStoreActivity", "Loading store image from: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.ivProfile)
        } else {
            Log.d("MyStoreActivity", "No store image available")
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
            val intent = Intent(this, SellsActivity::class.java)
            startActivity(intent)
        }

        binding.layoutPerluTagihan.setOnClickListener {
            val intent = Intent(this, SellsActivity::class.java)
            startActivity(intent)
            //navigateToSellsFragment("pending")
        }

        binding.layoutPembayaran.setOnClickListener {
            val intent = Intent(this, SellsActivity::class.java)
            startActivity(intent)
            //navigateToSellsFragment("paid")
        }

        binding.layoutPerluDikirim.setOnClickListener {
            val intent = Intent(this, SellsActivity::class.java)
            startActivity(intent)
            //navigateToSellsFragment("processed")
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
                .replace(android.R.id.content, ChatListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh store data
            viewModel.loadMyStore()
            Toast.makeText(this, "Profil toko berhasil diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PROFILE_REQUEST_CODE = 100
    }

//    private fun navigateToSellsFragment(status: String) {
//        val sellsFragment = SellsListFragment.newInstance(status)
//        supportFragmentManager.beginTransaction()
//            .replace(android.R.id.content, sellsFragment)
//            .addToBackStack(null)
//            .commit()
//    }
}