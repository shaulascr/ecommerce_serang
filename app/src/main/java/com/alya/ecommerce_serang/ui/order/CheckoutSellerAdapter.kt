package com.alya.ecommerce_serang.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.databinding.ItemOrderSellerBinding
import com.bumptech.glide.Glide

// Adapter for seller section that contains the product
class CheckoutSellerAdapter(private val checkoutData: CheckoutData) :
    RecyclerView.Adapter<CheckoutSellerAdapter.SellerViewHolder>() {

    class SellerViewHolder(val binding: ItemOrderSellerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerViewHolder {
        val binding = ItemOrderSellerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SellerViewHolder(binding)
    }

    override fun getItemCount(): Int = 1 // Only one seller based on your JSON

    override fun onBindViewHolder(holder: SellerViewHolder, position: Int) {
        with(holder.binding) {
            tvListProductOrder.text = checkoutData.sellerName

            // Load seller image
            Glide.with(ivSellerOrder.context)
                .load(checkoutData.sellerImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(ivSellerOrder)

            // Set up nested RecyclerView for the product
            rvSellerOrderProduct.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = CheckoutProductAdapter(checkoutData)
                isNestedScrollingEnabled = false
            }
        }
    }
}