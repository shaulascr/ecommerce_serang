package com.alya.ecommerce_serang.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.response.ProductsItem
import com.alya.ecommerce_serang.databinding.ItemProductHorizontalBinding
import com.bumptech.glide.Glide

class HorizontalProductAdapter(
    private var products: List<ProductsItem>,
    private val onClick: (ProductsItem) -> Unit
) : RecyclerView.Adapter<HorizontalProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductsItem) = with(binding) {
            itemName.text = product.name
            itemPrice.text = product.price
            rating.text = product.rating
//            productSold.text = "${product.totalSold} sold"

            // Load image using Glide
            Glide.with(itemView)
//                .load("${BuildConfig.BASE_URL}/product/${product.image}")
//                .load("${BuildConfig.BASE_URL}/${product.image}")
                .load(product.image)
                .into(image)

            root.setOnClickListener { onClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    fun updateProducts(newProducts: List<ProductsItem>) {
        products = newProducts
        notifyDataSetChanged()
    }
}