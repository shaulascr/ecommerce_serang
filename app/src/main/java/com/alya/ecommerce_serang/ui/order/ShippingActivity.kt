package com.alya.ecommerce_serang.ui.order

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CostProduct
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityCheckoutBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class ShippingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ShippingAdapter

    private val viewModel: ShippingViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            ShippingViewModel(orderRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_shipment_order)
        adapter = ShippingAdapter { selectedService ->
            val intent = Intent().apply {
                putExtra("ship_name", selectedService.service)
                putExtra("ship_price", selectedService.cost)
                putExtra("ship_service", selectedService.description)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val request = CourierCostRequest(
            addressId = intent.getIntExtra("extra_address_id", 0),
            itemCost = CostProduct(
                productId = intent.getIntExtra("product_id", 0),
                quantity = intent.getIntExtra("quantity", 1)
            )
        )

        viewModel.fetchShippingServices(request)

        lifecycleScope.launch {
            viewModel.shippingServices.collect { result ->
                result?.let {
                    when (it) {
                        is Result.Success -> adapter.submitList(it.data)
                        is Result.Error -> Toast.makeText(this@ShippingActivity, it.exception.message, Toast.LENGTH_SHORT).show()
                        is Result.Loading -> null
                    }
                }
            }
        }

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }
    }
}

//val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//    if (result.resultCode == RESULT_OK) {
//        val data = result.data
//        val shipName = data?.getStringExtra("ship_name")
//        val shipPrice = data?.getIntExtra("ship_price", 0)
//        val shipService = data?.getStringExtra("ship_service")
//        // use the data as needed
//    }
//}
//
//// launch the shipping activity
//val intent = Intent(this, ShippingActivity::class.java).apply {
//    putExtra("address_id", addressId)
//    putExtra("product_id", productId)
//    putExtra("quantity", quantity)
//}
//launcher.launch(intent)
