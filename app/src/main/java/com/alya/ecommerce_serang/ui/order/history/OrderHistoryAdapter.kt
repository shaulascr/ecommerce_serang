package com.alya.ecommerce_serang.ui.order.history

import android.app.Activity
import android.app.Dialog
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.OrdersItem
import com.alya.ecommerce_serang.data.api.dto.ReviewUIItem
import com.alya.ecommerce_serang.ui.order.detail.PaymentActivity
import com.alya.ecommerce_serang.ui.order.history.cancelorder.CancelOrderBottomSheet
import com.alya.ecommerce_serang.ui.order.review.CreateReviewActivity
import com.alya.ecommerce_serang.ui.product.ReviewProductActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class OrderHistoryAdapter(
    private val onOrderClickListener: (OrdersItem) -> Unit,
    private val viewModel: HistoryViewModel,
    private val callbacks: OrderActionCallbacks
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    interface OrderActionCallbacks {
        fun onOrderCancelled(orderId: String, success: Boolean, message: String)
        fun onOrderCompleted(orderId: Int, success: Boolean, message: String)
        fun onShowLoading(show: Boolean)
    }

    private val orders = mutableListOf<OrdersItem>()

    private var fragmentStatus: String = "all"

    fun setFragmentStatus(status: String) {
        fragmentStatus = status
    }

    fun submitList(newOrders: List<OrdersItem>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tvStoreName)
        private val rvOrderItems: RecyclerView = itemView.findViewById(R.id.rvOrderItems)
        private val tvShowMore: TextView = itemView.findViewById(R.id.tvShowMore)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvItemCountLabel: TextView = itemView.findViewById(R.id.tv_count_total_item)
//        private val tvDeadlineDate: TextView = itemView.findViewById(R.id.tvDeadlineDate)

        fun bind(order: OrdersItem) {
            // Get store name from the first order item
            val storeName = if (order.orderItems.isNotEmpty()) order.orderItems[0].storeName else ""
            tvStoreName.text = storeName

            // Set total amount
            tvTotalAmount.text = order.totalAmount

            // Set item count
            val itemCount = order.orderItems.size
            tvItemCountLabel.text = itemView.context.getString(R.string.item_count_prod, itemCount)

            // Set deadline date, adjust to each status
//            tvDeadlineDate.text = formatDate(order.updatedAt)

            // Set up the order items RecyclerView
            val productAdapter = OrderProductAdapter()
            rvOrderItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = productAdapter
            }

            // Display only the first product and show "View more" for the rest
            if (order.orderItems.isNotEmpty()) {
                productAdapter.submitList(order.orderItems.take(1))

                // Show or hide the "View more" text based on number of items
                if (order.orderItems.size > 1) {
                    val itemString = order.orderItems.size - 1
                    tvShowMore.visibility = View.VISIBLE
                    tvShowMore.text = itemView.context.getString(R.string.show_more_product, itemString)
                } else {
                    tvShowMore.visibility = View.GONE
                }
            } else {
                tvShowMore.visibility = View.GONE
            }

            // Set click listener for the entire order item
            itemView.setOnClickListener {
                onOrderClickListener(order)
            }

            val actualStatus = if (fragmentStatus == "all") order.displayStatus ?: "" else fragmentStatus
            adjustButtonsAndText(actualStatus, order)

        }

        private fun adjustButtonsAndText(status: String, order: OrdersItem) {
            Log.d("OrderHistoryAdapter", "Adjusting buttons for status: $status")
            // Mendapatkan referensi ke tombol-tombol
            val btnLeft = itemView.findViewById<MaterialButton>(R.id.btn_left)
            val btnRight = itemView.findViewById<MaterialButton>(R.id.btn_right)
            val statusOrder = itemView.findViewById<TextView>(R.id.tvOrderStatus)
            val deadlineLabel = itemView.findViewById<TextView>(R.id.tvDeadlineLabel)
            val deadlineDate = itemView.findViewById<TextView>(R.id.tvDeadlineDate)

            // Reset visibility
            btnLeft.visibility = View.GONE
            btnRight.visibility = View.GONE
            statusOrder.visibility = View.GONE
            deadlineLabel.visibility = View.GONE

            when (status) {
                "unpaid" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.unpaid_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_unpaid)
                    }
                    btnLeft.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.canceled_order_btn)
                        setOnClickListener {
                            showCancelOrderBottomSheet(order.orderId)
                        }
                    }

                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.sent_evidence)
                        setOnClickListener {
                            val intent = Intent(itemView.context, PaymentActivity::class.java)
                            // Menambahkan data yang diperlukan
                            intent.putExtra("ORDER_ID", order.orderId)
                            intent.putExtra("ORDER_PAYMENT_ID", order.paymentInfoId)

                            // Memulai aktivitas
                            itemView.context.startActivity(intent)
                        }
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDatePay(order.updatedAt)
                    }
                }
                "paid" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.paid_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_paid)
                    }
                    btnLeft.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.canceled_order_btn)
                        setOnClickListener {
                            showCancelOrderDialog(order.orderId.toString())
//                            viewModel.refreshOrders()
                        }
                    }
//                    deadlineDate.apply {
//                        visibility = View.VISIBLE
//                        text = formatDatePay(order.updatedAt)
//                    }
                }
                "processed" -> {
                    // Untuk status processed, tampilkan "Hubungi Penjual"
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.processed_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_processed)
                    }
                    // gabisa complaint
//                    btnLeft.apply {
//                        visibility = View.VISIBLE
//                        text = itemView.context.getString(R.string.canceled_order_btn)
//                        setOnClickListener {
//                            showCancelOrderDialog(order.orderId.toString())
//                            viewModel.refreshOrders()
//                        }
//                    }
                }
                "shipped" -> {
                    // Untuk status shipped, tampilkan "Lacak Pengiriman" dan "Terima Barang"
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.shipped_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_shipped)
                    }
                    btnLeft.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.claim_complaint)
                        setOnClickListener {
                            showCancelOrderDialog(order.orderId.toString())
//                            viewModel.refreshOrders()
                        }
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.claim_order)
                        setOnClickListener {
                            callbacks.onShowLoading(true)

                            // Call ViewModel
                            viewModel.confirmOrderCompleted(order.orderId, "completed")
//                            viewModel.refreshOrders()

                        }

                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatShipmentDate(order.updatedAt, order.etd ?: "0")
                    }
                }
                "completed" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.completed_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_shipped)
                    }
                    btnRight.apply {
                        val checkReview = order.orderItems[0].reviewId
                        if (checkReview > 0){
                            visibility = View.VISIBLE
                            text = itemView.context.getString(R.string.add_review)
                            setOnClickListener {

                                addReviewProduct(order)
//                            viewModel.refreshOrders()
                                // Handle click event
                            }
                        } else {
                            visibility = View.GONE
                        }


                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.updatedAt)
                    }
                }
                "canceled" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.canceled_orders)
                    }

                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_canceled)
                    }

                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.cancelDate)
                    }
                }
            }
        }

        private fun formatDate(dateString: String): String {
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

                    outputFormat.format(calendar.time)
                } ?: dateString
            } catch (e: Exception) {
                Log.e("DateFormatting", "Error formatting date: ${e.message}")
                dateString
            }
        }

        private fun formatDatePay(dateString: String): String {
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
                dueDateFormat.format(calendar.time)

            } catch (e: Exception) {
                Log.e("DateFormatting", "Error formatting date: ${e.message}")
                dateString
            }
        }

        private fun formatShipmentDate(dateString: String, estimate: String): String {
            return try {
                val estimateTD = if (estimate.isNullOrEmpty()) 0 else estimate.toInt()

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
                    calendar.add(Calendar.DAY_OF_MONTH, estimateTD)
                    outputFormat.format(calendar.time)
                } ?: dateString
            } catch (e: Exception) {
                Log.e("ShipmentDateFormatting", "Error formatting shipment date: ${e.message}")
                dateString
            }
        }

        private fun showCancelOrderDialog(orderId: String) {
            val context = itemView.context
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_cancel_order)
            dialog.setCancelable(true)

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

            // Set up the reasons dropdown
            val reasons = context.resources.getStringArray(R.array.cancellation_reasons)
            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, reasons)
            spinnerCancelReason.setAdapter(adapter)

            // For storing the selected image URI
            var selectedImageUri: Uri? = null

            // Set click listener for image selection
            ivComplaintImage.setOnClickListener {
                // Create an intent to open the image picker
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                (context as? Activity)?.startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK)

                // Set up result handler in the activity
                val activity = context as? Activity
                activity?.let {
                    // Remove any existing callbacks to avoid memory leaks
                    if (imagePickCallback != null) {
                        imagePickCallback = null
                    }

                    // Create a new callback for this specific dialog
                    imagePickCallback = { uri ->
                        selectedImageUri = uri

                        // Load and display the selected image
                        ivComplaintImage.setImageURI(uri)
                        tvSelectImage.visibility = View.GONE
                    }
                }
            }

            // Set click listeners for buttons
            btnCancelDialog.setOnClickListener {
                dialog.dismiss()
            }

            btnConfirmCancel.setOnClickListener {
                val reason = spinnerCancelReason.text.toString().trim()

                if (reason.isEmpty()) {
                    tilCancelReason.error = context.getString(R.string.please_select_cancellation_reason)
                    return@setOnClickListener
                }

                // Clear error if any
                tilCancelReason.error = null

                // Convert selected image to file if available
                val imageFile = selectedImageUri?.let { uri ->
                    try {
                        // Get the file path from URI
                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                        val cursor = context.contentResolver.query(uri, filePathColumn, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val columnIndex = it.getColumnIndex(filePathColumn[0])
                                val filePath = it.getString(columnIndex)
                                return@let File(filePath)
                            }
                        }
                        null
                    } catch (e: Exception) {
                        Log.e("OrderHistoryAdapter", "Error getting file from URI: ${e.message}")
                        null
                    }
                }

                callbacks.onShowLoading(true)

                // Call ViewModel method but don't observe here
                viewModel.cancelOrderWithImage(orderId, reason, imageFile)

                // Create a one-time observer that will be removed automatically
                val observer = object : Observer<Boolean> {
                    override fun onChanged(isSuccess: Boolean) {
                        callbacks.onShowLoading(false)

                        if (isSuccess) {
                            val message = viewModel.message.value ?: context.getString(R.string.order_canceled_successfully)
                            callbacks.onOrderCancelled(orderId, true, message)
                            dialog.dismiss()
                        } else {
                            val message = viewModel.message.value ?: context.getString(R.string.failed_to_cancel_order)
                            callbacks.onOrderCancelled(orderId, false, message)
                        }

                        // Remove this observer after first use
                        viewModel.isSuccess.removeObserver(this)
                    }
                }

                // Add observer only once
                viewModel.isSuccess.observe(itemView.findViewTreeLifecycleOwner()!!, observer)
            }
            dialog.show()
        }

        private fun showCancelOrderBottomSheet(orderId : Int) {
            val context = itemView.context

            // We need a FragmentManager to show the bottom sheet
            // Try to get it from the context
            val fragmentActivity = when (context) {
                is FragmentActivity -> context
                is ContextWrapper -> {
                    val baseContext = context.baseContext
                    if (baseContext is FragmentActivity) {
                        baseContext
                    } else {
                        // Log error and show a Toast instead if we can't get a FragmentManager
                        Log.e("OrderHistoryAdapter", "Cannot show bottom sheet: Context is not a FragmentActivity")
                        Toast.makeText(context, "Cannot show cancel order dialog", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                else -> {
                    // Log error and show a Toast instead if we can't get a FragmentManager
                    Log.e("OrderHistoryAdapter", "Cannot show bottom sheet: Context is not a FragmentActivity")
                    Toast.makeText(context, "Cannot show cancel order dialog", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // cancel sebelum bayar
            val bottomSheet = CancelOrderBottomSheet(
                orderId = orderId,
                onOrderCancelled = {
                    callbacks.onOrderCancelled(orderId.toString(), true, "Order cancelled successfully")
                    // Show a success message
                    Toast.makeText(context, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                }
            )

            bottomSheet.show(fragmentActivity.supportFragmentManager, CancelOrderBottomSheet.TAG)
        }

        // tambah review / ulasan
        private fun addReviewProduct(order: OrdersItem) {
            // Use ViewModel to fetch order details
            viewModel.getOrderDetails(order.orderId)

            // Create loading dialog
//            val loadingDialog = Dialog(itemView.context).apply {
//                requestWindowFeature(Window.FEATURE_NO_TITLE)
//                setContentView(R.layout.dialog_loading)
//                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//                setCancelable(false)
//            }
//            loadingDialog.show()

            viewModel.error.observe(itemView.findViewTreeLifecycleOwner()!!) { errorMsg ->
                if (!errorMsg.isNullOrEmpty()) {
                    Toast.makeText(itemView.context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            // Observe order items
            viewModel.orderItems.observe(itemView.findViewTreeLifecycleOwner()!!) { orderItems ->
                if (orderItems != null && orderItems.isNotEmpty()) {
                    // For single item review
                    if (orderItems.size == 1) {
                        val item = orderItems[0]
                        val intent = Intent(itemView.context, CreateReviewActivity::class.java).apply {
                            putExtra("order_item_id", item.orderItemId)
                            putExtra("product_name", item.productName)
                            putExtra("product_image", item.productImage)
                        }
                        (itemView.context as Activity).startActivityForResult(intent, REQUEST_CODE_REVIEW)
                    }
                    // For multiple items
                    else {
                        val reviewItems = orderItems.map { item ->
                            ReviewUIItem(
                                orderItemId = item.orderItemId,
                                productName = item.productName,
                                productImage = item.productImage
                            )
                        }

                        val itemsJson = Gson().toJson(reviewItems)
                        val intent = Intent(itemView.context, ReviewProductActivity::class.java).apply {
                            putExtra("order_items", itemsJson)
                        }
                        (itemView.context as Activity).startActivityForResult(intent, REQUEST_CODE_REVIEW)
                    }
                } else {
                    Toast.makeText(
                        itemView.context,
                        "No items to review",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
        const val REQUEST_CODE_REVIEW = 101
        private var imagePickCallback: ((Uri) -> Unit)? = null

        // This method should be called from the activity's onActivityResult
        fun handleImageResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
                val selectedImageUri = data.data
                selectedImageUri?.let { uri ->
                    imagePickCallback?.invoke(uri)
                }
            }
        }
    }
}