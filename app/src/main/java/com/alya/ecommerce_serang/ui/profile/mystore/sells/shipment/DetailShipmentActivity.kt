package com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.response.store.sells.Orders
import com.alya.ecommerce_serang.data.api.response.store.sells.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.AddressRepository
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailShipmentBinding
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsProductAdapter
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.AddressViewModel
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.getValue

class DetailShipmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailShipmentBinding
    private lateinit var sessionManager: SessionManager
    private var sells: Orders? = null
    private lateinit var productAdapter: SellsProductAdapter

    private val viewModel: SellsViewModel by viewModels {
        BaseViewModelFactory {
            sessionManager = SessionManager(this)
            val apiService = ApiConfig.getApiService(sessionManager)
            val sellsRepository = SellsRepository(apiService)
            SellsViewModel(sellsRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailShipmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Detail Pesanan"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

        val sellsJson = intent.getStringExtra("sells_data")
        if (sellsJson != null) {
            try {
                val basicOrder = Gson().fromJson(sellsJson, OrdersItem::class.java)
                basicOrder.orderId.let {
                    viewModel.getSellDetails(it)
                }
            } catch (e: Exception) {
                Log.e("DetailSellsActivity", "Failed to parse order data", e)
            }
        } else {
            Log.e("DetailSellsActivity", "No order passed in intent")
        }

        observeOrderDetails()

        binding.btnConfirmShipment.setOnClickListener {
            sells?.let {
                val intent = Intent(this, ShipmentConfirmationActivity::class.java)
                intent.putExtra("sells_data", Gson().toJson(it))
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Data pesanan tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeOrderDetails() {
        viewModel.sellDetails.observe(this) { order ->
            if (order != null) {
                sells = order
                showOrderDetails()
            } else {
                Log.e("DetailShipmentActivity", "❌ Failed to retrieve order details")
            }
        }
    }

    private fun showOrderDetails() = with(binding) {
        sells?.let { sell ->
            tvOrderNumber.text = sell.orderId.toString()
            tvOrderCustomer.text = sell.username
            tvOrderDate.text = formatDate(sell.updatedAt.toString())
            tvOrderTotalProduct.text = "(${sell.orderItems?.size ?: 0} Barang)"
            tvOrderSubtotal.text = formatPrice(sell.totalAmount.toString())
            tvOrderShipPrice.text = formatPrice(sell.shipmentPrice.toString())
            tvOrderPrice.text = formatPrice(sell.totalAmount.toString())
            tvOrderRecipient.text = sell.recipient ?: "-"
            tvOrderRecipientNum.text = sell.receiptNum?.toString() ?: "-"

            val cityId = sell.cityId?.toString()
            val provinceId = sell.provinceId?.toString()

            if (cityId != null && provinceId != null) {
                val viewModelAddress: AddressViewModel by viewModels {
                    BaseViewModelFactory {
                        val apiService = ApiConfig.getApiService(sessionManager)
                        val addressRepository = AddressRepository(apiService)
                        AddressViewModel(addressRepository)
                    }
                }

                viewModelAddress.fetchCities(provinceId)
                viewModelAddress.fetchProvinces()

                viewModelAddress.cities.observe(this@DetailShipmentActivity) { cities ->
                    val cityName = cities.find { it.cityId == cityId }?.cityName
                    viewModelAddress.provinces.observe(this@DetailShipmentActivity) { provinces ->
                        val provinceName = provinces.find { it.provinceId == provinceId }?.provinceName

                        val fullAddress = listOfNotNull(
                            sell.street,
                            sell.subdistrict,
                            cityName,
                            provinceName
                        ).joinToString(", ")

                        tvOrderRecipientAddress.text = fullAddress
                    }
                }
            } else {
                tvOrderRecipientAddress.text = "-"
            }

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
        return String.format(Locale("id", "ID"), "Rp%,.0f", priceDouble)
    }
}