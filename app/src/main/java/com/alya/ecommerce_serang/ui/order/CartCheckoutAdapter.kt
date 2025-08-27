package com.alya.ecommerce_serang.ui.order

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
import com.alya.ecommerce_serang.databinding.ItemOrderProductBinding
import com.alya.ecommerce_serang.databinding.ItemOrderSellerBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class CartCheckoutAdapter(
    private val checkoutData: CheckoutData
) : RecyclerView.Adapter<CartCheckoutAdapter.SellerViewHolder>() {

    private var productImages: Map<Int, String> = emptyMap()
    private val viewHolders = mutableListOf<SellerViewHolder>() // Keep references

    class SellerViewHolder(val binding: ItemOrderSellerBinding) : RecyclerView.ViewHolder(binding.root) {
        val childAdapter = MultiCartItemsAdapter(emptyList(), emptyMap())
        init {
            binding.rvSellerOrderProduct.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = childAdapter
                isNestedScrollingEnabled = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerViewHolder {
        val binding = ItemOrderSellerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SellerViewHolder(binding)
        viewHolders.add(holder) // Keep reference
        return holder
    }

    fun updateProductImages(newImages: Map<Int, String>) {
        productImages = newImages
        // Update all existing child adapters
        viewHolders.forEach { holder ->
            holder.childAdapter.updateProductImages(newImages)
        }
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: SellerViewHolder, position: Int) {
        holder.binding.tvStoreName.text = checkoutData.sellerName
        holder.childAdapter.updateData(checkoutData.cartItems)
        holder.childAdapter.updateProductImages(productImages) // Apply current images
    }

    override fun onViewRecycled(holder: SellerViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder) // Clean up reference
    }
}

class MultiCartItemsAdapter(
    private var cartItems: List<CartItemsItem> = emptyList(),
    private var productImages: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<MultiCartItemsAdapter.CartItemViewHolder>() {

    class CartItemViewHolder(val binding: ItemOrderProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartItemViewHolder(binding)
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateProductImages(images: Map<Int, String>) {
        Log.d("MultiCartItemsAdapter", "updateProductImages called with: $images")
        Log.d("MultiCartItemsAdapter", "Current cartItems productIds: ${cartItems.map { it.productId }}")
        productImages = images
        notifyDataSetChanged()
        Log.d("MultiCartItemsAdapter", "notifyDataSetChanged() called")
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val item = cartItems[position]
        Log.d("MultiCartItemsAdapter", "onBindViewHolder - position: $position, productId: ${item.productId}")
        Log.d("MultiCartItemsAdapter", "Available images: $productImages")

        with(holder.binding) {
            tvProductName.text = item.productName
            tvProductQuantity.text = "${item.quantity} buah"
            tvProductPrice.text = formatCurrency(item.price.toDouble())

            val img = productImages[item.productId]
            Log.d("MultiCartItemsAdapter", "Image for productId ${item.productId}: $img")

            val fullImageUrl = when (img) {
                is String -> {
                    val url = if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                    Log.d("MultiCartItemsAdapter", "Full image URL: $url")
                    url
                }
                else -> {
                    Log.d("MultiCartItemsAdapter", "No image found, using placeholder")
                    null
                }
            }

            Log.d("MultiCartItemsAdapter", "Loading image with Glide: $fullImageUrl")
            Glide.with(ivProduct.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)  // Add error handling
                .into(ivProduct)
        }
    }

    // Minimal helpers to update adapter data from parent adapter
    fun updateData(items: List<CartItemsItem>) {
        cartItems = items
        notifyDataSetChanged()
    }


    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }
}
