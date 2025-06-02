package com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.response.store.sells.OrdersItem
import com.alya.ecommerce_serang.databinding.ActivityDetailShipmentBinding
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsProductAdapter
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetailShipmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailShipmentBinding
    private lateinit var sells: OrdersItem
    private lateinit var productAdapter: SellsProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailShipmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderJson = intent.getStringExtra("sells_data")
        sells = Gson().fromJson(orderJson, OrdersItem::class.java)

        setupRecyclerView()
        bindOrderDetails()
    }

    private fun bindOrderDetails() = with(binding) {
        tvOrderNumber.text = sells.orderId.toString()
        tvOrderCustomer.text = sells.username
        tvOrderDate.text = formatDate(sells.createdAt)
        tvOrderDue.text = formatDate(sells.updatedAt)

        tvOrderTotalProduct.text = "(${sells.orderItems?.size ?: 0} Barang)"
        tvOrderSubtotal.text = "Rp${sells.totalAmount}"
        tvOrderPrice.text = "Rp${sells.totalAmount}"
        tvOrderShipPrice.text = "Rp${sells.shipmentPrice}"

        tvOrderRecipient.text = sells.username
//        tvOrderRecipientNum.text = sells.phone
        tvOrderRecipientAddress.text = sells.street
    }

    private fun setupRecyclerView() {
        productAdapter = SellsProductAdapter()

        binding.rvProductItems.apply {
            layoutManager = LinearLayoutManager(this@DetailShipmentActivity)
            adapter = productAdapter
        }

        // Submit the order items to the adapter
        productAdapter.submitList(sells.orderItems ?: emptyList())
    }

    private fun formatDate(dateStr: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val date = inputFormat.parse(dateStr ?: "")
            outputFormat.format(date!!)
        } catch (e: Exception) {
            "-"
        }
    }
}