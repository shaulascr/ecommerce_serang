package com.alya.ecommerce_serang.ui.product.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.databinding.ItemProductGridBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductsCategoryAdapter(
    private var products: List<ProductsItem>,
    private val onClick: (ProductsItem) -> Unit
) : RecyclerView.Adapter<ProductsCategoryAdapter.ProductViewHolder>() {

    fun updateProducts(newProducts: List<ProductsItem>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(
        private val binding: ItemProductGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductsItem) {
            binding.apply {
                tvProductName.text = product.name
                val priceValue = product.price.toDoubleOrNull() ?: 0.0
                tvProductPrice.text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(priceValue.toInt())}"
                // Load product image
                Glide.with(itemView.context)
                    .load("${BuildConfig.BASE_URL}${product.image}")
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivProductImage)

                // Set click listener
                root.setOnClickListener {
                    onClick(product)
                }

//                // Optional: Show stock status
//                if (product.stock > 0) {
//                    tvStockStatus.text = "Stock: ${product.stock}"
//                    tvStockStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
//                } else {
//                    tvStockStatus.text = "Out of Stock"
//                    tvStockStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
//                }
            }
        }
    }
}