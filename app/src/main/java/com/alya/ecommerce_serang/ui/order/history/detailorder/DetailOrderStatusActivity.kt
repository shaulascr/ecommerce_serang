package com.alya.ecommerce_serang.ui.order.history.detailorder

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CompletedOrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrdersItem
import com.alya.ecommerce_serang.data.api.dto.ReviewUIItem
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderListItemsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.Orders
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailOrderStatusBinding
import com.alya.ecommerce_serang.ui.order.detail.PaymentActivity
import com.alya.ecommerce_serang.ui.order.history.cancelorder.CancelOrderBottomSheet
import com.alya.ecommerce_serang.ui.order.review.CreateReviewActivity
import com.alya.ecommerce_serang.ui.product.ReviewProductActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DetailOrderStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailOrderStatusBinding
    private lateinit var sessionManager: SessionManager

    private var orderId: Int = -1
    private var orderStatus: String = ""
    private val orders = mutableListOf<OrdersItem>()
    private var selectedImageUri: Uri? = null

    private var cancelDialog: Dialog? = null
    private var dialogImageView: ImageView? = null
    private var dialogSelectTextView: TextView? = null

    private val viewModel: DetailOrderViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            DetailOrderViewModel(orderRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting activity initialization")

        binding = ActivityDetailOrderStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        orderId = intent.getIntExtra("ORDER_ID", -1)
        orderStatus = intent.getStringExtra("ORDER_STATUS") ?: ""

        Log.d(TAG, "onCreate: orderID=$orderId, orderStatus=$orderStatus")

        if (orderId == -1) {
            Log.e(TAG, "onCreate: Invalid order ID received")
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupObservers()
        loadOrderDetails()

        Log.d(TAG, "onCreate: Activity initialization completed")
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up LiveData observers")

        // Observe order details
        viewModel.orderDetails.observe(this) { orders ->
            if (orders != null) {
                Log.d(TAG, "Observer: orderDetails received, orderId=${orders.orderId}")
                populateOrderDetails(orders)
            } else {
                Log.w(TAG, "Observer: orderDetails is null")
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Observer: isLoading=$isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.error.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Log.e(TAG, "Observer: Error received: $errorMsg")
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe success status
        viewModel.isSuccess.observe(this) { isSuccess ->
            Log.d(TAG, "Observer: isSuccess=$isSuccess")
        }

        // Observe messages
        viewModel.message.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Log.d(TAG, "Observer: Message: $message")
            }
        }
    }

    private fun loadOrderDetails() {
        Log.d(TAG, "loadOrderDetails: Requesting order details for orderId=$orderId")
        viewModel.getOrderDetails(orderId)
    }

    private fun populateOrderDetails(orders: Orders) {
        Log.d(TAG, "populateOrderDetails: Populating UI with order data")

        try {
            // Set order date and payment deadline
            binding.tvOrderDate.text = formatDate(orders.createdAt)
            binding.tvPaymentDeadline.text = formatDatePay(orders.updatedAt)

            Log.d(TAG, "populateOrderDetails: Order created at ${orders.createdAt}, formatted as ${binding.tvOrderDate.text}")

            // Set address information
            binding.tvRecipientName.text = orders.detail
            binding.tvAddress.text = "${orders.street}, ${orders.subdistrict}"

            Log.d(TAG, "populateOrderDetails: Shipping to ${orders.detail} at ${orders.street}")

            // Set courier info
            binding.tvCourier.text = "${orders.courier} ${orders.service}"

            Log.d(TAG, "populateOrderDetails: Courier=${orders.courier}, Service=${orders.service}")

            // Set product details using RecyclerView
            Log.d(TAG, "populateOrderDetails: Setting up products RecyclerView with ${orders.orderItems.size} items")
            setupProductsRecyclerView(orders.orderItems)

            // Set payment method
            binding.tvPaymentMethod.text = "Bank Transfer - ${orders.payInfoName ?: "Tidak tersedia"}"

            Log.d(TAG, "populateOrderDetails: Payment method=${orders.payInfoName ?: "Tidak tersedia"}")

            // Set subtotal, shipping cost, and total
            val subtotal = orders.totalAmount?.toIntOrNull()?.minus(orders.shipmentPrice.toIntOrNull() ?: 0) ?: 0
            binding.tvSubtotal.text = "Rp$subtotal"
            binding.tvShippingCost.text = "Rp${orders.shipmentPrice}"
            binding.tvTotal.text = "Rp${orders.totalAmount}"

            Log.d(TAG, "populateOrderDetails: Subtotal=$subtotal, Shipping=${orders.shipmentPrice}, Total=${orders.totalAmount}")

            // Adjust buttons based on order status
            Log.d(TAG, "populateOrderDetails: Adjusting buttons for status=$orderStatus")
            adjustButtonsBasedOnStatus(orders, orderStatus)

        } catch (e: Exception) {
            Log.e(TAG, "populateOrderDetails: Error while populating UI", e)
            Toast.makeText(this, "Error loading order details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProductsRecyclerView(orderItems: List<OrderListItemsItem>) {
        Log.d(TAG, "setupProductsRecyclerView: Setting up RecyclerView with ${orderItems.size} items")

        val adapter = DetailOrderItemsAdapter()
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@DetailOrderStatusActivity)
            this.adapter = adapter
        }
        adapter.submitList(orderItems)
    }

    private fun adjustButtonsBasedOnStatus(orders: Orders, status: String) {
        Log.d(TAG, "adjustButtonsBasedOnStatus: Adjusting UI for status=$status")

        // Reset button visibility first
        binding.btnPrimary.visibility = View.GONE
        binding.btnSecondary.visibility = View.GONE

        // Set status header
        val statusText = when(status) {
            "pending" -> "Belum Bayar"
            "unpaid" -> "Belum Bayar"
            "processed" -> "Diproses"
            "shipped" -> "Dikirim"
            "delivered" -> "Diterima"
            "completed" -> "Selesai"
            "canceled" -> "Dibatalkan"
            else -> "Detail Pesanan"
        }

        binding.tvStatusHeader.text = statusText
        Log.d(TAG, "adjustButtonsBasedOnStatus: Status header set to '$statusText'")

        when (status) {
            "pending"->{
                binding.tvStatusHeader.text = "Menunggu Tagihan"
                binding.tvStatusNote.visibility = View.VISIBLE
                binding.tvStatusNote.text = "Pesanan ini harus dibayar sebelum ${formatDatePay(orders.updatedAt)}"

                // Set buttons
                binding.btnSecondary.apply {
                    visibility = View.VISIBLE
                    text = "Batalkan Pesanan"
                    setOnClickListener {
                        Log.d(TAG, "Cancel Order button clicked")
                        showCancelOrderBottomSheet(orders.orderId)
                        viewModel.getOrderDetails(orders.orderId)
                    }
                }
            }
            "unpaid" -> {
                Log.d(TAG, "adjustButtonsBasedOnStatus: Setting up UI for pending/unpaid order")

                // Show status note
                binding.tvStatusHeader.text = "Belum Dibayar"
                binding.tvStatusNote.visibility = View.VISIBLE
                binding.tvStatusNote.text = "Pesanan ini harus dibayar sebelum ${formatDatePay(orders.updatedAt)}"

                // Set buttons
                binding.btnSecondary.apply {
                    visibility = View.VISIBLE
                    text = "Batalkan Pesanan"
                    setOnClickListener {
                        Log.d(TAG, "Cancel Order button clicked")
                        showCancelOrderBottomSheet(orders.orderId)
                        viewModel.getOrderDetails(orders.orderId)
                    }
                }

                binding.btnPrimary.apply {
                    visibility = View.VISIBLE
                    text = "Bayar Sekarang"
                    setOnClickListener {
                        Log.d(TAG, "Pay Now button clicked, navigating to PaymentActivity")
                        val intent = Intent(this@DetailOrderStatusActivity, PaymentActivity::class.java)
                        intent.putExtra("ORDER_ID", orders.orderId)
                        intent.putExtra("ORDER_PAYMENT_ID", orders.paymentInfoId)
                        startActivity(intent)
                    }
                }
            }

            "processed" -> {
                Log.d(TAG, "adjustButtonsBasedOnStatus: Setting up UI for processed order")

                binding.tvStatusHeader.text = "Sedang Diproses"
                binding.tvStatusNote.visibility = View.VISIBLE
                binding.tvStatusNote.text = "Penjual sedang memproses pesanan Anda"

                binding.btnSecondary.apply {
                    visibility = View.VISIBLE
                    text = "Batalkan Pesanan"
                    setOnClickListener {
                        Log.d(TAG, "Cancel Order button clicked for processed order")
                        showCancelOrderDialog(orders.orderId.toString())
                        viewModel.getOrderDetails(orders.orderId)
                    }
                }

                binding.btnPrimary.apply {
                    visibility = View.GONE
                }
            }

            "shipped" -> {
                Log.d(TAG, "adjustButtonsBasedOnStatus: Setting up UI for shipped order")

                binding.tvStatusHeader.text = "Sudah Dikirim"
                binding.tvStatusNote.visibility = View.VISIBLE
                binding.tvStatusNote.text = "Pesanan Anda sedang dalam perjalanan. Akan sampai sekitar ${formatShipmentDate(orders.updatedAt, orders.etd ?: "0")}"

                binding.btnSecondary.apply {
                    visibility = View.VISIBLE
                    text = "Ajukan Komplain"
                    setOnClickListener {
                        Log.d(TAG, "Complaint button clicked")
                        showCancelOrderDialog(orders.orderId.toString())
                        viewModel.getOrderDetails(orders.orderId)
                    }
                }

                binding.btnPrimary.apply {
                    visibility = View.VISIBLE
                    text = "Terima Pesanan"

                    val completedOrderRequest = CompletedOrderRequest(
                        orderId = orders.orderId,
                        statusComplete = "completed"
                    )

                    setOnClickListener {
                        Log.d(TAG, "Confirm receipt button clicked, marking order as completed")
                        viewModel.confirmOrderCompleted(completedOrderRequest)
                    }
                }
            }

            "completed" -> {
                Log.d(TAG, "adjustButtonsBasedOnStatus: Setting up UI for delivered/completed order")

                binding.tvStatusHeader.text = "Pesanan Selesai"
                binding.tvStatusNote.visibility = View.GONE

                binding.btnPrimary.apply {
                    visibility = View.VISIBLE
                    text = "Beri Ulasan"
                    setOnClickListener {
                        Log.d(TAG, "Review button clicked")
                        addReviewForOrder(orders)
                        viewModel.getOrderDetails(orders.orderId)
                    }
                }
                binding.btnSecondary.apply {
                    visibility = View.GONE
                }
            }

            "canceled" -> {
                Log.d(TAG, "adjustButtonsBasedOnStatus: Setting up UI for canceled order")

                binding.tvStatusHeader.text = "Pesanan Selesai"
                binding.tvStatusNote.visibility = View.VISIBLE
                binding.tvStatusNote.text = "Pesanan dibatalkan: ${orders.cancelReason ?: "Alasan tidak diberikan"}"

                binding.btnSecondary.apply {
                    visibility = View.GONE
                }
                binding.btnPrimary.apply {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun addReviewForOrder(orders: Orders) {
        Log.d(TAG, "addReviewForOrder: Preparing to add review for order ${orders.orderId}")

        val orderItems = orders.orderItems

        if (orderItems.isNotEmpty()) {
            Log.d(TAG, "addReviewForOrder: Found ${orderItems.size} items to review")

            // For single item review
            if (orderItems.size == 1) {
                val item = orderItems[0]
                Log.d(TAG, "addReviewForOrder: Launching single item review for orderItemId=${item.orderItemId}")

                val intent = Intent(this, CreateReviewActivity::class.java).apply {
                    putExtra("order_item_id", item.orderItemId)
                    putExtra("product_name", item.productName)
                    putExtra("product_image", item.productImage)
                }
                startActivityForResult(intent, REQUEST_CODE_REVIEW)
            }
            // For multiple items
            else {
                Log.d(TAG, "addReviewForOrder: Launching multi-item review with ${orderItems.size} items")

                val reviewItems = orderItems.map { item ->
                    ReviewUIItem(
                        orderItemId = item.orderItemId,
                        productName = item.productName,
                        productImage = item.productImage
                    )
                }

                val itemsJson = Gson().toJson(reviewItems)
                Log.d(TAG, "addReviewForOrder: JSON prepared for items: ${itemsJson.take(100)}...")

                val intent = Intent(this, ReviewProductActivity::class.java).apply {
                    putExtra("order_items", itemsJson)
                }
                startActivityForResult(intent, REQUEST_CODE_REVIEW)
            }
        } else {
            Log.w(TAG, "addReviewForOrder: No items found to review")
            Toast.makeText(this, "No items to review", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCancelOrderDialog(orderId: String) {
        Log.d(TAG, "showCancelOrderDialog: Showing dialog for orderId=$orderId")

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_cancel_order)
        dialog.setCancelable(true)

        // Store dialog reference
        cancelDialog = dialog

        // Set the dialog width to match parent
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Get references to the views in the dialog
        val spinnerCancelReason = dialog.findViewById<AutoCompleteTextView>(R.id.spinnerCancelReason)
        val tilCancelReason = dialog.findViewById<TextInputLayout>(R.id.tilCancelReason)
        val btnCancelDialog = dialog.findViewById<MaterialButton>(R.id.btnCancelDialog)
        val btnConfirmCancel = dialog.findViewById<MaterialButton>(R.id.btnConfirmCancel)
        val ivComplaintImage = dialog.findViewById<ImageView>(R.id.ivComplaintImage)
        val tvSelectImage = dialog.findViewById<TextView>(R.id.tvSelectImage)

        dialogImageView = ivComplaintImage
        dialogSelectTextView = tvSelectImage

        // Set up the reasons dropdown
        val reasons = this.resources.getStringArray(R.array.cancellation_reasons)
        Log.d(TAG, "showCancelOrderDialog: Setting up dropdown with ${reasons.size} reasons")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reasons)
        spinnerCancelReason.setAdapter(adapter)

        // For storing the selected image URI
        var selectedImageUri: Uri? = null

        // Set click listener for image selection
        ivComplaintImage.setOnClickListener {
            Log.d(TAG, "showCancelOrderDialog: Image selection clicked")

            // Create an intent to open the image picker
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            (this as? Activity)?.startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK)

            // Set up result handler in the activity
            val activity = this as? Activity
            activity?.let {
                // Remove any existing callbacks to avoid memory leaks
                if (imagePickCallback != null) {
                    imagePickCallback = null
                }

                // Create a new callback for this specific dialog
                imagePickCallback = { uri ->
                    Log.d(TAG, "imagePickCallback: Image selected, URI=$uri")
                    selectedImageUri = uri

                    // Load and display the selected image
                    ivComplaintImage.setImageURI(uri)
                    tvSelectImage.visibility = View.GONE
                }
            }
        }

        // Set click listeners for buttons
        btnCancelDialog.setOnClickListener {
            Log.d(TAG, "showCancelOrderDialog: Cancel button clicked, dismissing dialog")
            dialog.dismiss()
        }

        btnConfirmCancel.setOnClickListener {
            val reason = spinnerCancelReason.text.toString().trim()
            Log.d(TAG, "showCancelOrderDialog: Confirm cancel clicked with reason: $reason")

            if (reason.isEmpty()) {
                Log.w(TAG, "showCancelOrderDialog: No reason selected")
                tilCancelReason.error = this.getString(R.string.please_select_cancellation_reason)
                return@setOnClickListener
            }

            // Clear error if any
            tilCancelReason.error = null

            // Convert selected image to file if available
            val imageFile = selectedImageUri?.let { uri ->
                try {
                    Log.d(TAG, "showCancelOrderDialog: Converting URI to file: $uri")
                    // Get the file path from URI
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = this.contentResolver.query(uri, filePathColumn, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val columnIndex = it.getColumnIndex(filePathColumn[0])
                            val filePath = it.getString(columnIndex)
                            Log.d(TAG, "showCancelOrderDialog: File path: $filePath")
                            return@let File(filePath)
                        }
                    }
                    Log.w(TAG, "showCancelOrderDialog: Failed to get file path from URI")
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "showCancelOrderDialog: Error getting file from URI: ${e.message}", e)
                    null
                }
            }

            // Show loading indicator
            Log.d(TAG, "showCancelOrderDialog: Showing loading indicator")
            val loadingView = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#80000000"))
            }

            dialog.addContentView(loadingView, loadingView.layoutParams)

            // Call the ViewModel to cancel the order with image
            Log.d(TAG, "showCancelOrderDialog: Calling cancelOrderWithImage for orderId=$orderId")
            viewModel.cancelOrderWithImage(orderId.toInt(), reason, imageFile)

            // Observe for success/failure
            viewModel.isSuccess.observe(this) { isSuccess ->
                Log.d(TAG, "showCancelOrderDialog observer: isSuccess=$isSuccess")

                if (isSuccess) {
                    Log.d(TAG, "showCancelOrderDialog: Order canceled successfully")
                    Toast.makeText(this, getString(R.string.order_canceled_successfully), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                    // Set result and finish
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Log.e(TAG, "showCancelOrderDialog: Failed to cancel order: ${viewModel.message.value}")
                    Toast.makeText(this, viewModel.message.value ?: getString(R.string.failed_to_cancel_order), Toast.LENGTH_SHORT).show()
                }
            }
        }

        Log.d(TAG, "showCancelOrderDialog: Dialog setup complete, showing dialog")
        dialog.show()
    }

    private fun showCancelOrderBottomSheet(orderId: Int) {
        // Create and show the bottom sheet directly since we're already in an Activity
        val bottomSheet = CancelOrderBottomSheet(
            orderId = orderId,
            onOrderCancelled = {
                // Handle the successful cancellation
                // Refresh the data

                // Show a success message
                Toast.makeText(this, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
            }
        )

        bottomSheet.show(supportFragmentManager, CancelOrderBottomSheet.TAG)
    }

    private fun formatDate(dateString: String): String {
        Log.d(TAG, "formatDate: Formatting date: $dateString")

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("HH:mm dd MMMM yyyy", Locale("id", "ID"))

            val date = inputFormat.parse(dateString)

            date?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)

                val formatted = outputFormat.format(calendar.time)
                Log.d(TAG, "formatDate: Formatted date: $formatted")
                formatted
            } ?: dateString
        } catch (e: Exception) {
            Log.e(TAG, "formatDate: Error formatting date: ${e.message}", e)
            dateString
        }
    }

    private fun formatDatePay(dateString: String): String {
        Log.d(TAG, "formatDatePay: Formatting payment date: $dateString")

        return try {
            // Parse the ISO 8601 date
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val createdDate = isoDateFormat.parse(dateString)

            // Add 24 hours to get due date
            val calendar = Calendar.getInstance()
            calendar.time = createdDate
            calendar.add(Calendar.HOUR, 24)
            val dueDate = calendar.time

            // Format due date for display
            val dueDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formatted = dueDateFormat.format(calendar.time)

            Log.d(TAG, "formatDatePay: Formatted payment date: $formatted")
            formatted

        } catch (e: Exception) {
            Log.e(TAG, "formatDatePay: Error formatting date: ${e.message}", e)
            dateString
        }
    }

    private fun formatShipmentDate(dateString: String, estimateString: String): String {
        Log.d(TAG, "formatShipmentDate: Formatting shipment date: $dateString with ETD: $estimateString")

        return try {
            // Safely parse the estimate to Int
            val estimate = if (estimateString.isNullOrEmpty()) 0 else estimateString.toInt()
            Log.d(TAG, "formatShipmentDate: Parsed ETD as $estimate days")

            // Parse the input date
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            // Output format
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

            // Parse the input date
            val date = inputFormat.parse(dateString)

            date?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it

                // Add estimated days
                calendar.add(Calendar.DAY_OF_MONTH, estimate)
                val formatted = outputFormat.format(calendar.time)

                Log.d(TAG, "formatShipmentDate: Estimated arrival date: $formatted")
                formatted
            } ?: dateString
        } catch (e: Exception) {
            Log.e(TAG, "formatShipmentDate: Error formatting shipment date: ${e.message}", e)
            dateString
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        when (requestCode) {
            REQUEST_IMAGE_PICK -> {
                if (resultCode == RESULT_OK && data != null) {
                    // Get the selected image URI
                    selectedImageUri = data.data
                    Log.d(TAG, "onActivityResult: Image selected, URI=$selectedImageUri")

                    // Update the image view in the dialog if the dialog is still showing
                    if (cancelDialog?.isShowing == true) {
                        Log.d(TAG, "onActivityResult: Updating image in dialog")
                        dialogImageView?.setImageURI(selectedImageUri)
                        dialogSelectTextView?.visibility = View.GONE
                    } else {
                        Log.w(TAG, "onActivityResult: Dialog is not showing, cannot update image")
                    }
                } else {
                    Log.w(TAG, "onActivityResult: Image selection canceled or failed")
                }
            }
            REQUEST_CODE_REVIEW -> {
                if (resultCode == RESULT_OK) {
                    // Review submitted successfully
                    Log.d(TAG, "onActivityResult: Review submitted successfully")
                    Toast.makeText(this, "Review submitted successfully", Toast.LENGTH_SHORT).show()

                    // Refresh order details
                    loadOrderDetails()

                    // Set result to notify parent activity
                    setResult(RESULT_OK)
                } else {
                    Log.w(TAG, "onActivityResult: Review submission canceled or failed")
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up references")
        super.onDestroy()
        // Clean up references
        cancelDialog = null
        dialogImageView = null
        dialogSelectTextView = null
    }


    companion object {
        private const val REQUEST_IMAGE_PICK = 100
        private const val REQUEST_CODE_REVIEW = 101
        private const val TAG = "DetailOrderActivity" // Add tag for logging


        private var imagePickCallback: ((Uri) -> Unit)? = null
    }
}