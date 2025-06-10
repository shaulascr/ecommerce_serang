package com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.response.store.sells.Orders
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivityShipmentConfirmationBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.gson.Gson
import kotlin.getValue

class ShipmentConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShipmentConfirmationBinding
    private var sells: Orders? = null
    private lateinit var sessionManager: SessionManager

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
        binding = ActivityShipmentConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Konfirmasi Pengiriman"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

        val sellsJson = intent.getStringExtra("sells_data")
        sells = Gson().fromJson(sellsJson, Orders::class.java)

        // Pre-fill fields
        binding.edtNoPesanan.setText(sells?.orderId?.toString())
        binding.edtKurir.setText(sells?.courier ?: "")
        binding.edtLayananKirim.setText(sells?.service ?: "")

        binding.btnConfirm.setOnClickListener {
            val receiptNum = binding.edtNoResi.text.toString().trim()
            val orderId = sells?.orderId

            if (receiptNum.isEmpty()) {
                Toast.makeText(this, "Nomor resi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else if (orderId == null) {
                Toast.makeText(this, "Order ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.confirmShipment(orderId, receiptNum)
            }
        }

        viewModel.message.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.isSuccess.observe(this) { success ->
            if (success) finish()
        }
    }
}