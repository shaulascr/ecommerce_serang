package com.alya.ecommerce_serang.ui.profile.mystore.sells.payment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailPaymentBinding
import com.alya.ecommerce_serang.ui.profile.mystore.sells.SellsProductAdapter
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetailPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPaymentBinding
    private lateinit var sells: OrdersItem
    private lateinit var productAdapter: SellsProductAdapter
    private lateinit var sessionManager: SessionManager


    private val viewModel: SellsViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val sellsRepository = SellsRepository(apiService)
            SellsViewModel(sellsRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val orderJson = intent.getStringExtra("sells_data")
        sells = Gson().fromJson(orderJson, OrdersItem::class.java)

        binding.header.headerLeftIcon.setOnClickListener {
//            onBackPressed()
            finish()
        }

        setupRecyclerView()
        bindOrderDetails()
        setupPaymentEvidenceViewer()

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
//        sells.paymentEvidence

        binding.btnConfirmPayment.setOnClickListener{
            viewModel.confirmPayment(sells.orderId, "confirmed")
            finish()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = SellsProductAdapter()

        binding.rvProductItems.apply {
            layoutManager = LinearLayoutManager(this@DetailPaymentActivity)
            adapter = productAdapter
        }

        // Submit the order items to the adapter
        productAdapter.submitList(sells.orderItems ?: emptyList())
    }

    private fun setupPaymentEvidenceViewer() {
        binding.tvPaymentDueDesc.setOnClickListener {
            val paymentEvidence = sells.paymentEvidence
            if (!paymentEvidence.isNullOrEmpty()) {
                showPaymentEvidenceDialog(paymentEvidence)
            } else {
                Toast.makeText(this, "Bukti pembayaran tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPaymentEvidenceDialog(paymentEvidence: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_viewer)
        dialog.setCancelable(true)

        // Set dialog to fullscreen
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        // Get views from dialog
        val imageView = dialog.findViewById<ImageView>(R.id.iv_payment_evidence)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)
        val tvTitle = dialog.findViewById<TextView>(R.id.tv_title)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progress_bar)

        // Set title
        tvTitle.text = "Bukti Pembayaran"

        // Build image URL
        val fullImageUrl = when (val img = paymentEvidence) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image // Default image for null
        }

        // Show progress bar while loading
        progressBar.visibility = View.VISIBLE

        // Load image with Glide
        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: com.bumptech.glide.request.transition.Transition<in Drawable>?) {
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


        // Close button click listener
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Click outside to close
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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