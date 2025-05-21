package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.orders.OrderItemsItem
import com.bumptech.glide.Glide

class SellsProductAdapter(
    private val items: List<OrderItemsItem?>
) : RecyclerView.Adapter<SellsProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.iv_order_product)
        val tvName: TextView = view.findViewById(R.id.tv_order_product_name)
        val tvQty: TextView = view.findViewById(R.id.tv_order_product_qty)
        val tvPrice: TextView = view.findViewById(R.id.tv_order_product_price)
        val tvTotal: TextView = view.findViewById(R.id.tv_order_product_total_price)

        fun bind(item: OrderItemsItem) {
            tvName.text = item.productName
            tvQty.text = "${item.quantity} x "
            tvPrice.text = "Rp${item.price}"
            val total = (item.quantity ?: 1) * (item.price ?: 0)
            tvTotal.text = "Rp$total"
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
}