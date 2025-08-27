package com.alya.ecommerce_serang.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.databinding.ItemOrderSellerBinding

// Adapter for seller section that contains the product
class CheckoutSellerAdapter(private val checkoutData: CheckoutData) :
    RecyclerView.Adapter<CheckoutSellerAdapter.SellerViewHolder>() {

    private var productImages: Map<Int, String> = emptyMap()
    private var currentViewHolder: SellerViewHolder? = null

    class SellerViewHolder(val binding: ItemOrderSellerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerViewHolder {
        val binding = ItemOrderSellerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val holder = SellerViewHolder(binding)
        currentViewHolder = holder
        return holder
    }

    fun updateProductImages(newImages: Map<Int, String>) {
        productImages = newImages
        currentViewHolder?.let { holder ->
            // Update the nested adapter
            val adapter = holder.binding.rvSellerOrderProduct.adapter
            when (adapter) {
                is SingleCartItemAdapter -> adapter.updateProductImages(newImages)
                is SingleProductAdapter -> {
                    // For SingleProductAdapter, you might need to update differently
                    // since it uses checkoutData.productImageUrl
                }
            }
        }
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: SellerViewHolder, position: Int) {
        currentViewHolder = holder
        with(holder.binding) {
            tvStoreName.text = checkoutData.sellerName

            rvSellerOrderProduct.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = if (checkoutData.isBuyNow) {
                    SingleProductAdapter(checkoutData)
                } else {
                    SingleCartItemAdapter(checkoutData.cartItems.first()).also { adapter ->
                        // Apply existing images if available
                        if (productImages.isNotEmpty()) {
                            adapter.updateProductImages(productImages)
                        }
                    }
                }
                isNestedScrollingEnabled = false
            }
        }
    }
}