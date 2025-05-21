package com.alya.ecommerce_serang.ui.order.history.detailorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderListItemsItem
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class DetailOrderItemsAdapter : RecyclerView.Adapter<DetailOrderItemsAdapter.DetailOrderItemViewHolder>() {

    private val items = mutableListOf<OrderListItemsItem>()

    fun submitList(newItems: List<OrderListItemsItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailOrderItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail_product, parent, false)
        return DetailOrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailOrderItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DetailOrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(item: OrderListItemsItem) {
            // Load product image
            Glide.with(itemView.context)
                .load(item.productImage)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivProduct)

            val newPrice = formatCurrency(item.price.toDouble())

            tvProductName.text = item.productName
            tvQuantity.text = "${item.quantity} buah"
            tvPrice.text = "Rp${newPrice}"
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }
}