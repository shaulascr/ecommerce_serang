package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.ui.profile.mystore.sells.payment.DetailPaymentActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment.DetailShipmentActivity
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SellsAdapter(
    private val onOrderClickListener: (OrdersItem) -> Unit,
    private val viewModel: SellsViewModel
) : RecyclerView.Adapter<SellsAdapter.SellsViewHolder>() {

    private val sells = mutableListOf<OrdersItem>()
    private var fragmentStatus: String = "all"

    fun setFragmentStatus(status: String) {
        fragmentStatus = status
    }

    fun submitList(newSells: List<OrdersItem>) {
        Log.d("SellsAdapter", "submitList called with ${newSells.size} items")
        sells.clear()
        sells.addAll(newSells)
        notifyDataSetChanged()
        Log.d("SellsAdapter", "Adapter updated. Current size: ${sells.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellsViewHolder {
        Log.d("SellsAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sells, parent, false)
        Log.d("SellsAdapter", "View inflated successfully")
        return SellsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SellsViewHolder, position: Int) {
        Log.d("SellsAdapter", "onBindViewHolder called for position: $position")
        Log.d("SellsAdapter", "Total items in adapter: ${sells.size}")
        if (position < sells.size) {
            holder.bind(sells[position])
        } else {
            Log.e("SellsAdapter", "Position $position is out of bounds for size ${sells.size}")
        }
    }

    override fun getItemCount(): Int = sells.size

    inner class SellsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tvUserName)
        private val rvOrderItems: RecyclerView = itemView.findViewById(R.id.rvSellsItems)
        private val tvShowMore: TextView = itemView.findViewById(R.id.tvShowMores)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmounts)
        private val tvItemCountLabel: TextView = itemView.findViewById(R.id.tv_count_total_items)

        fun bind(order: OrdersItem) {
            Log.d("SellsAdapter", "=== ViewHolder.bind() called ===")
            Log.d("SellsAdapter", "Binding order: ${order.orderId} with status: ${order.status}")

            // Show customer/buyer name (seller's perspective)
            tvStoreName.text = order.username ?: "Unknown Customer"
            Log.d("SellsAdapter", "Customer name set: ${order.username}")

            // Set total amount
            tvTotalAmount.text = "Rp${order.totalAmount}"
            Log.d("SellsAdapter", "Total amount set: ${order.totalAmount}")

            // Set item count
            val itemCount = order.orderItems?.size ?: 0
            tvItemCountLabel.text = itemView.context.getString(R.string.item_count_prod, itemCount)
            Log.d("SellsAdapter", "Item count set: $itemCount")

            // Set up the order items RecyclerView
            val productAdapter = SellsProductAdapter()
            rvOrderItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = productAdapter
            }
            Log.d("SellsAdapter", "Product RecyclerView configured")

            // Display only the first product and show "View more" for the rest
            order.orderItems?.let { items ->
                if (items.isNotEmpty()) {
                    productAdapter.submitList(items.take(1))
                    Log.d("SellsAdapter", "Product list submitted: ${items.size} items")

                    // Show or hide the "View more" text based on number of items
                    if (items.size > 1) {
                        val itemString = items.size - 1
                        tvShowMore.visibility = View.VISIBLE
                        tvShowMore.text = itemView.context.getString(R.string.show_more_product, itemString)
                        Log.d("SellsAdapter", "Show more visible: $itemString more items")
                    } else {
                        tvShowMore.visibility = View.GONE
                        Log.d("SellsAdapter", "Show more hidden: only 1 item")
                    }
                } else {
                    tvShowMore.visibility = View.GONE
                    Log.w("SellsAdapter", "Order has no items!")
                }
            } ?: run {
                tvShowMore.visibility = View.GONE
                Log.w("SellsAdapter", "Order items is null!")
            }

            // Set click listener for the entire order item
            itemView.setOnClickListener {
                onOrderClickListener(order)
            }

            val actualStatus = if (fragmentStatus == "all") order.status ?: "" else fragmentStatus
            Log.d("SellsAdapter", "Adjusting UI for status: '$actualStatus' (fragmentStatus: '$fragmentStatus')")
            adjustButtonsAndText(actualStatus, order)

            Log.d("SellsAdapter", "=== ViewHolder.bind() completed ===")
        }

        private fun adjustButtonsAndText(status: String, order: OrdersItem) {
            Log.d("SellsAdapter", "Adjusting buttons for status: $status")

            // Get references to buttons and status views
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
            deadlineDate.visibility = View.GONE

            when (status) {
                "pending" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Menunggu Tagihan"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Batas waktu konfirmasi:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.createdAt ?: "")
                    }
                    btnLeft.apply {
                        visibility = View.VISIBLE
                        text = "Tolak Pesanan"
                        setOnClickListener {
                            // Handle reject order
                            viewModel.updateOrderStatus(order.orderId, "canceled")
                            viewModel.refreshOrders()
                        }
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = "Terima Pesanan"
                        setOnClickListener {
                            // Handle accept order
                            viewModel.updateOrderStatus(order.orderId, "unpaid")
                            viewModel.refreshOrders()
                        }
                    }
                }
                "unpaid" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Konfirmasi Bayar"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Batas pembayaran:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDatePay(order.updatedAt ?: "")
                    }
                    btnLeft.apply {
                        visibility = View.VISIBLE
                        text = "Batalkan"
                        setOnClickListener {
                            viewModel.updateOrderStatus(order.orderId, "canceled")
                            viewModel.refreshOrders()
                        }
                    }
                }
                "paid" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Sudah Dibayar"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Konfirmasi pembayaran sebelum:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDatePay(order.updatedAt ?: "")
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = "Konfirmasi Pembayaran"
                        setOnClickListener {
                            val context = itemView.context
                            val intent = Intent(context, DetailPaymentActivity::class.java)
                            intent.putExtra("sells_data", Gson().toJson(order))
                            context.startActivity(intent)
                        }
                    }
                }
                "processed" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Diproses"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Kirim sebelum:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDatePay(order.updatedAt ?: "")
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = "Kirim Pesanan"
                        setOnClickListener {
                            val context = itemView.context
                            val intent = Intent(context, DetailShipmentActivity::class.java)
                            intent.putExtra("sells_data", Gson().toJson(order))
                            context.startActivity(intent)
                        }
                    }
                }
                "shipped" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Dikirim"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Dikirimkan pada:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.updatedAt ?: "")
                    }
                }
                "completed" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Selesai"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Selesai pada:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.updatedAt ?: "")
                    }
                }
                "canceled" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = "Dibatalkan"
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = "Dibatalkan pada:"
                    }
                    deadlineDate.apply {
                        visibility = View.VISIBLE
                        text = formatDate(order.cancelDate ?: "")
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

                // Format due date for display
                val dueDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dueDateFormat.format(calendar.time)

            } catch (e: Exception) {
                Log.e("DateFormatting", "Error formatting date: ${e.message}")
                dateString
            }
        }
    }
}