package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.sells.OrderItemsItem
import com.bumptech.glide.Glide
import java.util.Locale

class SellsProductAdapter : RecyclerView.Adapter<SellsProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<OrderItemsItem?>()

    fun submitList(newItems: List<OrderItemsItem?>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.iv_order_product)
        val tvName: TextView = view.findViewById(R.id.tv_order_product_name)
        val tvQty: TextView = view.findViewById(R.id.tv_order_product_qty)
        val tvPrice: TextView = view.findViewById(R.id.tv_order_product_price)
        val tvTotal: TextView = view.findViewById(R.id.tv_order_product_total_price)

        fun bind(item: OrderItemsItem) {
            tvName.text = item.productName
            tvQty.text = "${item.quantity} x "
            tvPrice.text = formatPrice(item.price.toString())
            tvTotal.text = formatPrice(item.subtotal.toString())
            Glide.with(ivProduct.context)
                .load(item.productImage)
                .placeholder(R.drawable.placeholder_image)
                .into(ivProduct)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sells_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        items[position]?.let { holder.bind(it) }
    }

    private fun formatPrice(price: String): String {
        val priceDouble = price.toDoubleOrNull() ?: 0.0
        val formattedPrice = String.format(Locale("id", "ID"), "Rp%,.0f", priceDouble)
        return formattedPrice
    }
}