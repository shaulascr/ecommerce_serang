package com.alya.ecommerce_serang.ui.order.address

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

        setupToolbar()

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
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
//    private fun updateEmptyState(isEmpty: Boolean) {
//        binding.layoutEmptyAddresses.isVisible = isEmpty
//        binding.rvAddresses.isVisible = !isEmpty
//    }

    private fun onBackPressedWithResult() {
        viewModel.selectedAddressId.value?.let {
            val intent = Intent()
            intent.putExtra(EXTRA_ADDRESS_ID, it)
            setResult(RESULT_OK, intent)
        }
        finish()
    }

    companion object {
        const val EXTRA_ADDRESS_ID = "extra_address_id"
    }
}

//override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//    super.onActivityResult(requestCode, resultCode, data)
//    if (requestCode == REQUEST_ADDRESS && resultCode == RESULT_OK) {
//        val selectedAddressId = data?.getIntExtra("selected_address_id", -1)
//        // Use the selected address ID
//    }
//}
