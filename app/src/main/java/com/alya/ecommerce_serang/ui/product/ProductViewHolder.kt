package com.alya.ecommerce_serang.ui.product

import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.Product
import com.alya.ecommerce_serang.databinding.ItemProductHorizontalBinding
import com.bumptech.glide.Glide

class ProductViewHolder(private val binding: ItemProductHorizontalBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        product: Product,
        onClick: (product: Product) -> Unit,
    ) = with(binding) {
        Glide.with(root).load(product.image).into(image)

//        discount.isVisible = product.discount != null
//        product.discount?.let {
//            val discount = (product.discount / product.price * 100).roundToInt()
//            binding.discount.text =
//                root.context.getString(R.string.fragment_item_product_discount, discount)
//        }

        itemName.text = product.title
        rating.text = String.format("%.1f", product.rating)

//        val current = product.price - (product.discount ?: 0.0)
        val current = product.price
        itemPrice.text = root.context.getString(R.string.item_price_txt, current)

        root.setOnClickListener {
            onClick(product)
        }
    }
}