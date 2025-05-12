package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.bumptech.glide.Glide
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
        sells.clear()
        sells.addAll(newSells)
        notifyDataSetChanged()
    }

    fun findResource(status: String): Int {
        return when (status) {
            "pending" -> R.layout.item_sells_order
            "paid" -> R.layout.item_sells_payment
            "processed" -> R.layout.item_sells_shipment
            else -> R.layout.item_sells_payment
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(findResource(fragmentStatus), parent, false)
        return SellsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SellsViewHolder, position: Int) {
        holder.bind(sells[position])
    }

    override fun getItemCount(): Int = sells.size

    inner class SellsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSellsTitle: TextView = itemView.findViewById(R.id.tv_sells_title)
        private val tvSellsNumber: TextView = itemView.findViewById(R.id.tv_sells_number)
        private val tvSellsDueDesc: TextView = itemView.findViewById(R.id.tv_sells_due_desc)
        private val tvSellsDue: TextView = itemView.findViewById(R.id.tv_sells_due)
        private val tvSellsLocation: TextView = itemView.findViewById(R.id.tv_sells_location)
        private val tvSellsCustomer: TextView = itemView.findViewById(R.id.tv_sells_customer)
        private val ivSellsProduct: ImageView = itemView.findViewById(R.id.iv_sells_product)
        private val tvSellsProductName: TextView = itemView.findViewById(R.id.tv_sells_product_name)
        private val tvSellsProductQty: TextView = itemView.findViewById(R.id.tv_sells_product_qty)
        private val tvSellsProductPrice: TextView = itemView.findViewById(R.id.tv_sells_product_price)
        private val tvSeeMore: TextView = itemView.findViewById(R.id.tv_see_more)
        private val tvSellsQty: TextView = itemView.findViewById(R.id.tv_sells_qty)
        private val tvSellsPrice: TextView = itemView.findViewById(R.id.tv_sells_price)
        private val btnEditOrder: Button = itemView.findViewById(R.id.btn_edit_order)
        private val btnConfirmOrder: Button = itemView.findViewById(R.id.btn_confirm_order)
        private val btnConfirmPayment: Button = itemView.findViewById(R.id.btn_confirm_payment)
        private val btnConfirmShipment: Button = itemView.findViewById(R.id.btn_confirm_shipment)

        fun bind(sells: OrdersItem) {

            tvSellsNumber.text = "No. Pesanan: ${sells.orderId}"
            tvSellsLocation.text = sells.subdistrict
            tvSellsCustomer.text = sells.username

            val product = sells.orderItems?.get(0)
            product?.let {
                tvSellsProductName.text = it.productName
                tvSellsProductQty.text = "x${it.quantity}"
                tvSellsProductPrice.text = "Rp${it.price}"

                Glide.with(itemView.context)
                    .load(it.productImage)
                    .placeholder(R.drawable.placeholder_image)
                    .into(ivSellsProduct)
            }

            sells.orderItems?.size?.let {
                if (it > 1) {
                    tvSeeMore.visibility = View.VISIBLE
                    tvSeeMore.text = "Lihat ${it.minus(1)} produk lainnya"
                } else {
                    tvSeeMore.visibility = View.GONE
                }
            }

            tvSellsQty.text = "${sells.orderItems?.size} produk"
            tvSellsPrice.text = "Rp${sells.totalAmount}"

            adjustDisplay(fragmentStatus, sells)
        }

        private fun adjustDisplay(status: String, sells: OrdersItem) {
            Log.d("SellsAdapter", "Adjusting display for status: $status")

            when (status) {
                "pending" -> {
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 3)
                    btnEditOrder.setOnClickListener {
                        TODO("Go to DetailOrderActivity")
                    }
                    btnConfirmOrder.setOnClickListener {
                        viewModel.updateOrderStatus(sells.orderId, "unpaid")
                    }
                }
                "paid" -> {
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 1)
                    btnConfirmPayment.setOnClickListener {
                        TODO("Go to DetailPaymentActivity")
                    }
                }
                "processed" -> {
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 2)
                    btnConfirmShipment.setOnClickListener {
                        TODO("Go to DetailShipmentActivity")
                    }
                }
                "shipped" -> {
                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "delivered" -> {
                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "completed" -> {
                    tvSellsTitle.text = "Pesanan Selesai"
                    tvSellsDueDesc.text = "Selesai pada"
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "canceled" -> {
                    tvSellsTitle.text = "Pesanan Dibatalkan"
                    tvSellsDueDesc.text = "Dibatalkan pada"
                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
            }
        }

        private fun formatDueDate(date: String, dueDay: Int): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val outputFormat = SimpleDateFormat("dd MM; HH.mm", Locale("id", "ID"))

                val date = inputFormat.parse(date)

                date?.let {
                    val calendar = Calendar.getInstance()
                    calendar.time = it
                    calendar.add(Calendar.DATE, dueDay)

                    outputFormat.format(calendar.time)
                } ?: date
            } catch (e: Exception) {
                Log.e("DueDateFormatting", "Error formatting date: ${e.message}")
                date
            }.toString()
        }
    }
}