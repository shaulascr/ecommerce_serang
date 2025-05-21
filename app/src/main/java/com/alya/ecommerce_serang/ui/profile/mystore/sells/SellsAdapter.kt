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
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.ui.profile.mystore.sells.payment.DetailPaymentActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment.DetailShipmentActivity
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.bumptech.glide.Glide
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
        sells.clear()
        sells.addAll(newSells)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sells, parent, false)
        return SellsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SellsViewHolder, position: Int) {
        holder.bind(sells[position])
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

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 1)
                }
                "paid" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 2)
                    btnConfirmPayment.setOnClickListener {
                        val context = itemView.context
                        val intent = Intent(context, DetailPaymentActivity::class.java)
                        intent.putExtra("sells_data", Gson().toJson(sells))
                        context.startActivity(intent)
                    }
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

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 2)
                    btnConfirmShipment.setOnClickListener {
                        val context = itemView.context
                        val intent = Intent(context, DetailShipmentActivity::class.java)
                        intent.putExtra("sells_data", Gson().toJson(sells))
                        context.startActivity(intent)
                    }
                }
                "shipped" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "delivered" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Telah Dikirim"
                    tvSellsDueDesc.text = "Dikirimkan pada"

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "completed" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

                    tvSellsTitle.text = "Pesanan Selesai"
                    tvSellsDueDesc.text = "Selesai pada"

                    tvSellsDue.text = formatDueDate(sells.updatedAt.toString(), 0)
                    tvSellsDue.background = itemView.context.getDrawable(R.drawable.bg_product_inactive)
                    btnConfirmPayment.visibility = View.GONE
                }
                "canceled" -> {
                    layoutOrders.visibility = View.GONE
                    layoutPayments.visibility = View.VISIBLE
                    layoutShipments.visibility = View.GONE

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