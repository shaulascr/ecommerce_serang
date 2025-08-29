package com.alya.ecommerce_serang.ui.profile.mystore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityMyStoreBinding
import com.alya.ecommerce_serang.ui.profile.mystore.balance.BalanceActivity
import com.alya.ecommerce_serang.ui.profile.mystore.chat.ChatListStoreActivity
import com.alya.ecommerce_serang.ui.profile.mystore.product.ProductActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.DetailStoreProfileActivity
import com.alya.ecommerce_serang.ui.profile.mystore.review.ReviewActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

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

        binding.headerMyStore.headerTitle.text = "Toko Saya"

        binding.headerMyStore.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

        viewModel.myStoreProfile.observe(this){ user ->
            user?.let { myStoreProfileOverview(it.store) }
        }

        viewModel.loadMyStore()
        viewModel.loadMyStoreProducts()
        viewModel.fetchBalance()

        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
        setUpClickListeners()
        getCountOrder()
        observeViewModel()
        fetchBalance()
    }

    private fun myStoreProfileOverview(store: Store){

        binding.tvStoreName.text = store.storeName
        binding.tvStoreType.text = store.storeType

        val imageUrl = when {
            store.storeImage.toString().isBlank() -> null
            store.storeImage.toString().startsWith("http") == true -> store.storeImage
            store.storeImage.toString().startsWith("/") == true -> BASE_URL + store.storeImage.toString().removePrefix("/")
            else -> BASE_URL + store.storeImage
        }

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(binding.ivProfile)

//        binding.tvBalance.text = String.format("Rp%,.0f", store.balance.toString())
    }

    private fun setUpClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, DetailStoreProfileActivity::class.java))
        }

        binding.layoutBalance.setOnClickListener {
            startActivity(Intent(this, BalanceActivity::class.java))
        }

        binding.tvHistory.setOnClickListener {
            //startActivity(Intent(this, SellsActivity::class.java))
            startSellsActivityWithStatus("all")
        }

        binding.layoutPerluTagihan.setOnClickListener {
            //startActivity(Intent(this, SellsActivity::class.java))
            startSellsActivityWithStatus("unpaid")
        }

        binding.layoutPembayaran.setOnClickListener {
            //startActivity(Intent(this, SellsActivity::class.java))
            startSellsActivityWithStatus("paid")
        }

        binding.layoutPerluDikirim.setOnClickListener {
            //startActivity(Intent(this, SellsActivity::class.java))
            startSellsActivityWithStatus("processed")
        }

        binding.layoutProductMenu.setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }

        binding.layoutReview.setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }

        binding.layoutInbox.setOnClickListener {
            startActivity(Intent(this, ChatListStoreActivity::class.java))
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

    private fun getCountOrder(){
        lifecycleScope.launch {
            try {
                val allCounts = viewModel.getAllStatusCounts()
                val totalUnpaid    = allCounts["unpaid"]
                val totalPaid      = allCounts["paid"]
                val totalProcessed = allCounts["processed"]
                Log.d("MyStoreActivity",
                    "Total orders: unpaid=$totalUnpaid, processed=$totalProcessed, paid=$totalPaid")

                binding.tvNumPesananMasuk.text = totalUnpaid.toString()
                binding.tvNumPembayaran.text   = totalPaid.toString()
                binding.tvNumPerluDikirim.text = totalProcessed.toString()
            } catch (e:Exception){
                Log.e("MyStoreActivity", "Error getting order counts: ${e.message}")
            }
        }
    }

    private fun fetchBalance(){
        viewModel.balanceResult.observe(this){result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading ->
                    null
                is com.alya.ecommerce_serang.data.repository.Result.Success ->
                    viewModel.formattedBalance.observe(this) {
                        binding.tvBalance.text = it
                    }
                is Result.Error   -> {
                    Log.e(
                        "MyStoreActivity",
                        "Gagal memuat saldo: ${result.exception.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.productList.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    null
                }
                is Result.Success -> {
                    val productList = result.data
                    val count = productList.size
                    Log.d("MyStoreActivty", "You have $count products")

                    // Example: update UI
                    binding.tvNumProduct.text = "$count produk"
                }
                is Result.Error -> {
                    Log.e("MyStoreActivity", "Failed load product : ${result.exception.message}" )
                }
            }
        }
    }

    private fun startSellsActivityWithStatus(status: String?) {
        val intent = Intent(this, SellsActivity::class.java)
        intent.putExtra(SellsActivity.EXTRA_INITIAL_STATUS, status)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.getAllStatusCounts()
        }
        viewModel.loadMyStore()
        viewModel.loadMyStoreProducts()
        viewModel.fetchBalance()
    }

    companion object {
        private const val PROFILE_REQUEST_CODE = 100
    }

}