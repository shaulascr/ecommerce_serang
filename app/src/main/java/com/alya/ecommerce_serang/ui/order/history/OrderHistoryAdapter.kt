package com.alya.ecommerce_serang.ui.order.history

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.order.OrdersItem
import com.alya.ecommerce_serang.ui.order.detail.PaymentActivity
import com.google.android.material.button.MaterialButton

class OrderHistoryAdapter(
    private val onOrderClickListener: (OrdersItem) -> Unit
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

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
        private val tvDeadlineDate: TextView = itemView.findViewById(R.id.tvDeadlineDate)

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
            tvDeadlineDate.text = formatDate(order.createdAt)

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

            //adjust each fragment
            adjustButtonsAndText(fragmentStatus, order)

        }

        private fun getStatusLabel(status: String): String {
            return when (status.toLowerCase()) {
                "pending" -> itemView.context.getString(R.string.pending_orders)
                "unpaid" -> itemView.context.getString(R.string.unpaid_orders)
                "processed" -> itemView.context.getString(R.string.processed_orders)
                "paid" -> itemView.context.getString(R.string.paid_orders)
                "shipped" -> itemView.context.getString(R.string.shipped_orders)
                "delivered" -> itemView.context.getString(R.string.delivered_orders)
                "completed" -> itemView.context.getString(R.string.completed_orders)
                "canceled" -> itemView.context.getString(R.string.canceled_orders)
                else -> status
            }
        }

        private fun adjustButtonsAndText(status: String, order: OrdersItem) {
            Log.d("OrderHistoryAdapter", "Adjusting buttons for status: $status")
            // Mendapatkan referensi ke tombol-tombol
            val btnLeft = itemView.findViewById<MaterialButton>(R.id.btn_left)
            val btnRight = itemView.findViewById<MaterialButton>(R.id.btn_right)
            val statusOrder = itemView.findViewById<TextView>(R.id.tvOrderStatus)
            val deadlineLabel = itemView.findViewById<TextView>(R.id.tvDeadlineLabel)

            // Reset visibility
            btnLeft.visibility = View.GONE
            btnRight.visibility = View.GONE
            statusOrder.visibility = View.GONE
            deadlineLabel.visibility = View.GONE

            when (status) {
                "pending" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.pending_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_pending)
                    }
                }
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
                            // Handle click event
                        }
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.claim_order)
                        setOnClickListener {
                            // Handle click event
                        }
                    }
                }
                "delivered" -> {
                    // Untuk status delivered, tampilkan "Beri Ulasan"
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.add_review)
                        setOnClickListener {
                            // Handle click event
                        }
                    }
                }
                "completed" -> {
                    statusOrder.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.shipped_orders)
                    }
                    deadlineLabel.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.dl_shipped)
                    }
                    btnRight.apply {
                        visibility = View.VISIBLE
                        text = itemView.context.getString(R.string.add_review)
                        setOnClickListener {
                            // Handle click event
                        }
                    }
                }
            }
        }

        private fun formatDate(dateString: String): String {
            // In a real app, you would parse the date string and format it
            // For this example, just return the string as is
            return dateString
        }
    }
}