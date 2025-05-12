package com.alya.ecommerce_serang.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.databinding.ItemProductGridBinding
import com.bumptech.glide.Glide

class SearchResultsAdapter(
    private val onItemClick: (ProductsItem) -> Unit
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
            binding.productName.text = product.name
            binding.productPrice.text = (product.price)

            // Load image with Glide
            Glide.with(binding.root.context)
                .load(product.image)
                .placeholder(R.drawable.placeholder_image)
//                .error(R.drawable.error_image)
                .into(binding.productImage)

            // Set store name if available
            product.storeId?.toString().let {
                binding.storeName.text = it
            }
        }
    }

    override fun submitList(list: List<ProductsItem>?) {
        Log.d("SearchResultsAdapter", "Submitting list with ${list?.size ?: 0} items")
        super.submitList(list)
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