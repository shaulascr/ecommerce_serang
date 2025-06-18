package com.alya.ecommerce_serang.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.databinding.ItemProductGridBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class SearchResultsAdapter(
    private val onItemClick: (ProductsItem) -> Unit,
    private val storeMap: Map<Int, StoreItem>
) : ListAdapter<ProductsItem, SearchResultsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ViewHolder(private val binding: ItemProductGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(product: ProductsItem) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = formatCurrency(product.price.toDouble())

            val fullImageUrl = if (product.image.startsWith("/")) {
                BASE_URL + product.image.removePrefix("/") // Append base URL if the path starts with "/"
            } else {
                product.image // Use as is if it's already a full URL
            }
            Log.d("ProductAdapter", "Loading image: $fullImageUrl")
            // Load image with Glide
            Glide.with(binding.root.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
//                .error(R.drawable.error_image)
                .into(binding.ivProductImage)

            val storeName = product.storeId?.let { storeMap[it]?.storeName } ?: "Unknown Store"
            binding.tvStoreName.text = storeName
        }
    }

    override fun submitList(list: List<ProductsItem>?) {
        Log.d("SearchResultsAdapter", "Submitting list with ${list?.size ?: 0} items")
        super.submitList(list)
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProductsItem>() {
            override fun areItemsTheSame(oldItem: ProductsItem, newItem: ProductsItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProductsItem, newItem: ProductsItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}