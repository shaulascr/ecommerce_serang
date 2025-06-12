package com.alya.ecommerce_serang.ui.profile.mystore.sells.payment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.sells.Orders
import com.alya.ecommerce_serang.data.api.response.store.sells.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.AddressRepository
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailPaymentBinding
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsProductAdapter
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.AddressViewModel
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.ui.profile.mystore.sells.DetailSellsActivity

class DetailPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPaymentBinding
    private var sells: Orders? = null
    private lateinit var productAdapter: SellsProductAdapter
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
        binding = ActivityDetailPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.headerTitle.text = "Detail Pesanan"
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

        productAdapter = SellsProductAdapter()
        binding.rvProductItems.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(this@DetailPaymentActivity)
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

        binding.tvOrderSellsDesc.setOnClickListener {
            val paymentEvidence = sells?.paymentEvidence
            if (!paymentEvidence.isNullOrEmpty()) {
                showPaymentEvidenceDialog(paymentEvidence)
            } else {
                Toast.makeText(this, "Bukti pembayaran tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHoldPayment.setOnClickListener {
            sells?.orderId?.let {
                viewModel.confirmPayment(it, "onhold")
                Toast.makeText(this, "Otomatis pembayaran dinonaktifkan", Toast.LENGTH_SHORT).show()
            } ?: run {
                Log.e("DetailPaymentActivity", "No order passed in intent")
            }
        }

        binding.btnConfirmPayment.setOnClickListener {
            sells?.orderId?.let {
                viewModel.confirmPayment(it, "confirmed")
                Toast.makeText(this, "Pembayaran dikonfirmasi", Toast.LENGTH_SHORT).show()
            } ?: run {
                Log.e("DetailPaymentActivity", "No order passed in intent")
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

                viewModelAddress.cities.observe(this@DetailPaymentActivity) { cities ->
                    val cityName = cities.find { it.cityId == cityId }?.cityName
                    viewModelAddress.provinces.observe(this@DetailPaymentActivity) { provinces ->
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

    private fun showPaymentEvidenceDialog(paymentEvidence: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_viewer)
        dialog.setCancelable(true)

        // Set dialog to fullscreen
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawable(Color.WHITE.toDrawable())

        // Get views from dialog
        val imageView = dialog.findViewById<ImageView>(R.id.iv_payment_evidence)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)
        val tvTitle = dialog.findViewById<TextView>(R.id.tv_title)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progress_bar)

        tvTitle.text = "Bukti Pembayaran"
        val fullImageUrl =
            if (paymentEvidence.startsWith("/")) BASE_URL + paymentEvidence.substring(1)
            else paymentEvidence

        progressBar.visibility = View.VISIBLE

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    progressBar.visibility = View.GONE
                    imageView.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    progressBar.visibility = View.GONE
                    imageView.setImageDrawable(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    progressBar.visibility = View.GONE
                    imageView.setImageDrawable(errorDrawable)
                    Toast.makeText(this@DetailPaymentActivity, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }
            })

        btnClose.setOnClickListener { dialog.dismiss() }
        imageView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}