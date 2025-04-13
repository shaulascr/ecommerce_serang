package com.alya.ecommerce_serang.ui.order.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.order.OrdersItem

class OrderHistoryAdapter(
    private val onOrderClickListener: (OrdersItem) -> Unit
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<OrdersItem>()

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
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val rvOrderItems: RecyclerView = itemView.findViewById(R.id.rvOrderItems)
        private val tvShowMore: TextView = itemView.findViewById(R.id.tvShowMore)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvItemCountLabel: TextView = itemView.findViewById(R.id.tv_count_total_item)
        private val tvDeadlineDate: TextView = itemView.findViewById(R.id.tvDeadlineDate)

        fun bind(order: OrdersItem) {
            // Get store name from the first order item
            val storeName = if (order.orderItems.isNotEmpty()) order.orderItems[0].storeName else ""
            tvStoreName.text = storeName

            // Set order status based on shipment status
            tvOrderStatus.text = getStatusLabel(order.shipmentStatus)

            // Set total amount
            tvTotalAmount.text = order.totalAmount

            // Set item count
            val itemCount = order.orderItems.size
            tvItemCountLabel.text = itemView.context.getString(R.string.item_count_prod, itemCount)

            // Set deadline date
            tvDeadlineDate.text = formatDate(order.createdAt) // This would need a proper date formatting function

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

        private fun formatDate(dateString: String): String {
            // In a real app, you would parse the date string and format it
            // For this example, just return the string as is
            return dateString
        }
    }
}