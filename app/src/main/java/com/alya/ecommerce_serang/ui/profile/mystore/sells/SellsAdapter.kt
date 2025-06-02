package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.ui.profile.mystore.sells.payment.DetailPaymentActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment.DetailShipmentActivity
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.bumptech.glide.Glide
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
        private val layoutOrders: View = itemView.findViewById(R.id.layout_orders)
        private val layoutPayments: View = itemView.findViewById(R.id.layout_payments)
        private val layoutShipments: View = itemView.findViewById(R.id.layout_shipments)

        private var tvSellsTitle: TextView = itemView.findViewById(R.id.tv_payment_title)
        private var tvSellsNumber: TextView = itemView.findViewById(R.id.tv_payment_number)
        private var tvSellsDueDesc: TextView = itemView.findViewById(R.id.tv_payment_due_desc)
        private var tvSellsDue: TextView = itemView.findViewById(R.id.tv_payment_due)
        private var tvSellsLocation: TextView = itemView.findViewById(R.id.tv_payment_location)
        private var tvSellsCustomer: TextView = itemView.findViewById(R.id.tv_payment_customer)
        private var ivSellsProduct: ImageView = itemView.findViewById(R.id.iv_payment_product)
        private var tvSellsProductName: TextView = itemView.findViewById(R.id.tv_payment_product_name)
        private var tvSellsProductQty: TextView = itemView.findViewById(R.id.tv_payment_product_qty)
        private var tvSellsProductPrice: TextView = itemView.findViewById(R.id.tv_payment_product_price)
        private var tvSeeMore: TextView = itemView.findViewById(R.id.tv_see_more_payment)
        private var tvSellsQty: TextView = itemView.findViewById(R.id.tv_payment_qty)
        private var tvSellsPrice: TextView = itemView.findViewById(R.id.tv_payment_price)
        private val btnConfirmPayment: Button = itemView.findViewById(R.id.btn_confirm_payment)
        private val btnConfirmShipment: Button = itemView.findViewById(R.id.btn_confirm_shipment)

        fun bind(order: OrdersItem) {
            Log.d("SellsAdapter", "=== ViewHolder.bind() called ===")
            Log.d("SellsAdapter", "Binding order: ${order.orderId} with status: ${order.status}")

            val actualStatus = if (fragmentStatus == "all") order.status ?: "" else fragmentStatus
            adjustDisplay(actualStatus, order)

            tvSellsNumber.text = "No. Pesanan: ${order.orderId}"
            tvSellsLocation.text = order.subdistrict
            tvSellsCustomer.text = order.username

            val product = order.orderItems?.firstOrNull()
            tvSellsProductName.text = product?.productName
            tvSellsProductQty.text = "x${product?.quantity}"
            tvSellsProductPrice.text = formatPrice(product?.price.toString())

            val fullImageUrl = when (val img = product?.productImage) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> R.drawable.placeholder_image
            }
            // Load product image using Glide
            Glide.with(itemView.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivSellsProduct)

            // Display only the first product and show "View more" for the rest
            order.orderItems?.let { items ->
                if (items.isNotEmpty()) {
                    // Show or hide the "View more" text based on number of items
                    if (items.size > 1) {
                        val itemString = items.size - 1
                        tvSeeMore.visibility = View.VISIBLE
                        tvSeeMore.text = itemView.context.getString(R.string.show_more_product, itemString)
                    } else {
                        tvSeeMore.visibility = View.GONE
                    }
                } else {
                    tvSeeMore.visibility = View.GONE
                    Log.w("SellsAdapter", "Order has no items!")
                }
            } ?: run {
                tvSeeMore.visibility = View.GONE
                Log.w("SellsAdapter", "Order items is null!")
            }

            // Set click listener for the entire order item
            itemView.setOnClickListener {
                onOrderClickListener(order)
            }

            Log.d("SellsAdapter", "=== ViewHolder.bind() completed ===")
        }

        private fun adjustDisplay(status: String, order: OrdersItem) {
            Log.d("SellsAdapter", "Adjusting display for status: $status")

            // Reset visibility
            layoutOrders.visibility = View.GONE
            layoutPayments.visibility = View.VISIBLE
            layoutShipments.visibility = View.GONE

            when (status) {
                "pending", "unpaid" -> {
                    layoutOrders.visibility = View.VISIBLE
                    layoutPayments.visibility = View.GONE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle = itemView.findViewById(R.id.tv_order_title)
                    tvSellsNumber = itemView.findViewById(R.id.tv_order_number)
                    tvSellsDue = itemView.findViewById(R.id.tv_order_due)
                    tvSellsCustomer = itemView.findViewById(R.id.tv_order_customer)
                    tvSellsProductName = itemView.findViewById(R.id.tv_order_product_name)
                    tvSellsProductQty = itemView.findViewById(R.id.tv_order_product_qty)
                    tvSellsProductPrice = itemView.findViewById(R.id.tv_order_product_price)
                    tvSeeMore = itemView.findViewById(R.id.tv_see_more_order)
                    tvSellsQty = itemView.findViewById(R.id.tv_order_qty)
                    tvSellsPrice = itemView.findViewById(R.id.tv_order_price)

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 1)

                    val product = order.orderItems?.firstOrNull()
                    tvSellsProductName.text = product?.productName
                    tvSellsProductQty.text = "x${product?.quantity}"
                    tvSellsProductPrice.text = formatPrice(product?.price.toString())

                    val fullImageUrl = when (val img = product?.productImage) {
                        is String -> {
                            if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                        }
                        else -> R.drawable.placeholder_image
                    }
                    // Load product image using Glide
                    Glide.with(itemView.context)
                        .load(fullImageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(ivSellsProduct)

                    tvSellsQty.text = "${order.orderItems?.size} produk"
                    tvSellsPrice.text = formatPrice(order.totalAmount.toString())
                }
                "paid" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 2)
                    btnConfirmPayment.setOnClickListener {

                        val context = itemView.context
                        val intent = Intent(context, DetailPaymentActivity::class.java)
                        intent.putExtra("sells_data", Gson().toJson(order))
                        context.startActivity(intent)
                        viewModel.refreshOrders()
                    }

                    tvSellsTitle.text = "Pesanan Telah Dibayar"
                    tvSellsDueDesc.text = "Konfirmasi pembayaran sebelum:"
                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 2)

                }
                "processed" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.GONE
                    layoutShipments.visibility = View.VISIBLE

                    tvSellsTitle = itemView.findViewById(R.id.tv_shipment_title)
                    tvSellsNumber = itemView.findViewById(R.id.tv_shipment_number)
                    tvSellsDue = itemView.findViewById(R.id.tv_shipment_due)
                    tvSellsLocation = itemView.findViewById(R.id.tv_shipment_location)
                    tvSellsCustomer = itemView.findViewById(R.id.tv_shipment_customer)
                    tvSellsProductName = itemView.findViewById(R.id.tv_shipment_product_name)
                    tvSellsProductQty = itemView.findViewById(R.id.tv_shipment_product_qty)
                    tvSeeMore = itemView.findViewById(R.id.tv_see_more_shipment)

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 2)
                    btnConfirmShipment.setOnClickListener {
                        val context = itemView.context
                        val intent = Intent(context, DetailShipmentActivity::class.java)
                        intent.putExtra("sells_data", Gson().toJson(order))
                        context.startActivity(intent)
                    }

                    tvSellsTitle.text = "Pesanan Perlu Dikirim"
                    tvSellsNumber.text = "No. Pesanan: ${order.orderId}"
                    tvSellsLocation.text = order.subdistrict
                    tvSellsCustomer.text = order.username
                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 2)
                }
                "shipped" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "delivered" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "completed" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Selesai"
                    tvSellsDueDesc.text = "Selesai pada"

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "canceled" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Dibatalkan"
                    tvSellsDueDesc.text = "Dibatalkan pada"

                    tvSellsDue.text = formatDueDate(order.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
            }
        }

        private fun formatDueDate(dateString: String, dueDay: Int): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val outputFormat = SimpleDateFormat("dd MMM; HH.mm", Locale("id", "ID"))

                val date = inputFormat.parse(dateString)

                date?.let {
                    val calendar = Calendar.getInstance()
                    calendar.time = it
                    calendar.add(Calendar.DATE, dueDay)

                    outputFormat.format(calendar.time)
                } ?: dateString
            } catch (e: Exception) {
                Log.e("DateFormatting", "Error formatting date: ${e.message}")
                dateString
            }
        }

        private fun formatPrice(price: String): String {
            val priceDouble = price.toDoubleOrNull() ?: 0.0
            val formattedPrice = String.format(Locale("id", "ID"), "Rp%,.0f", priceDouble)
            return formattedPrice
        }
    }
}