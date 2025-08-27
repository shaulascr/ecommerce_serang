package com.alya.ecommerce_serang.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
import com.alya.ecommerce_serang.databinding.ItemOrderProductBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class SingleCartItemAdapter(private val cartItem: CartItemsItem) :
    RecyclerView.Adapter<SingleCartItemAdapter.CartItemViewHolder>() {

    private var productImages: Map<Int, String> = emptyMap()

    class CartItemViewHolder(val binding: ItemOrderProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartItemViewHolder(binding)
    }

    override fun getItemCount(): Int = 1

    fun updateProductImages(newImages: Map<Int, String>) {
        productImages = newImages
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        with(holder.binding) {
            tvProductName.text = cartItem.productName
            tvProductQuantity.text = "${cartItem.quantity} buah"
            tvProductPrice.text = formatCurrency(cartItem.price.toDouble())

            // Get the image for this product
            val img = productImages[cartItem.productId]
            val fullImageUrl = when (img) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> null
            }

            Glide.with(ivProduct.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivProduct)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }
}
