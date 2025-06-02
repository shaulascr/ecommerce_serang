package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.response.store.sells.Orders
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailSellsBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.getValue

class DetailSellsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailSellsBinding
    private lateinit var sessionManager: SessionManager

    private var sells: Orders? = null
    private lateinit var productAdapter: SellsProductAdapter

    private val viewModel: SellsViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val sellsRepository = SellsRepository(apiService)
            SellsViewModel(sellsRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailSellsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Detail Pesanan"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

        productAdapter = SellsProductAdapter()
        binding.rvProductItems.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(this@DetailSellsActivity)
        }

        val sellsJson = intent.getStringExtra("sells")
        if (sellsJson != null) {
            try {
                sells = Gson().fromJson(sellsJson, Orders::class.java)
                showOrderDetails()
            } catch (e: Exception) {
                Log.e("DetailSellsActivity", "Failed to parse order data", e)
            }
        } else {
            Log.e("DetailSellsActivity", "Order data is missing")
        }
    }

    private fun showOrderDetails() {
        sells?.let { sell ->
            when (sell.orderStatus) {
                "pending", "unpaid" -> {
                    binding.tvOrderSellsTitle.text = "Pesanan Belum Dibayar"
                }
                "shipped" -> {
                    binding.tvOrderSellsTitle.text = "Pesanan Telah Dikirim"
                }
                "delivered" -> {
                    binding.tvOrderSellsTitle.text = "Pesanan Telah Sampai"
                }
                "completed" -> {
                    binding.tvOrderSellsTitle.text = "Pesanan Selesai"
                }
                "canceled" -> {
                    binding.tvOrderSellsTitle.text = "Pesanan Dibatalkan"
                }
            }

            binding.tvOrderNumber.text = sell.orderId.toString()
            binding.tvOrderCustomer.text = sell.username
            binding.tvOrderDate.text = formatDate(sell.updatedAt.toString())
            binding.tvOrderTotalProduct.text = "(${sell.orderItems?.size} Barang)"
            binding.tvOrderSubtotal.text = formatPrice(sell.totalAmount.toString())
            binding.tvOrderShipPrice.text = formatPrice(sell.shipmentPrice.toString())
            binding.tvOrderPrice.text = formatPrice(sell.totalAmount.toString())
            binding.tvOrderRecipient.text = sell.username
            binding.tvOrderRecipientNum.text = sell.receiptNum.toString()

            sell.orderItems?.let {
                productAdapter.submitList(it.filterNotNull())
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID"))

            val date = inputFormat.parse(dateString)

            date?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it

                outputFormat.format(calendar.time)
            } ?: dateString
        } catch (e: Exception) {
            Log.e("DateFormatting", "Error formatting date: ${e.message}")
            dateString
        }
    }

    private fun formatPrice(price: String): String {
        val priceDouble = price.toDoubleOrNull() ?: 0.0
        val formattedPrice = String.format(Locale("id", "ID"), "Rp%,.0f", priceDouble)
        return formattedPrice
    }
}