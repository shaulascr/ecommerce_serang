package com.alya.ecommerce_serang.ui.order

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.ActivityAddressBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class AddressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddressBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AddressAdapter

    private val viewModel: AddressViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            AddressViewModel(orderRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        adapter = AddressAdapter { selectedId ->
            viewModel.selectAddress(selectedId)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedWithResult()
        }

        binding.rvSellerOrder.layoutManager = LinearLayoutManager(this)
        binding.rvSellerOrder.adapter = adapter

        viewModel.fetchAddresses()

        viewModel.addresses.observe(this) { addressList ->
            adapter.submitList(addressList)
        }

        viewModel.selectedAddressId.observe(this) { selectedId ->
            adapter.setSelectedAddressId(selectedId)
        }
    }

    private fun onBackPressedWithResult() {
        viewModel.selectedAddressId.value?.let {
            val intent = Intent()
            intent.putExtra("selected_address_id", it)
            setResult(RESULT_OK, intent)
        }
        finish()
    }
}