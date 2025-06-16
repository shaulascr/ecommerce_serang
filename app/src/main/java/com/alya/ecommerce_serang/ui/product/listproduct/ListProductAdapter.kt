package com.alya.ecommerce_serang.ui.product.listproduct

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.databinding.ItemProductGridBinding
import com.bumptech.glide.Glide

class ListProductAdapter(
    private var products: List<ProductsItem>,
    private val onClick: (ProductsItem) -> Unit,
    private val storeMap: Map<Int, StoreItem>
): RecyclerView.Adapter<ListProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductsItem) = with(binding) {

            tvProductName.text = product.name
            tvProductPrice.text = product.price
            rating.text = product.rating

            val fullImageUrl = if (product.image.startsWith("/")) {
                BASE_URL + product.image.removePrefix("/") // Append base URL if the path starts with "/"
            } else {
                product.image // Use as is if it's already a full URL
            }
            Log.d("ProductAdapter", "Loading image: $fullImageUrl")


            // Load image using Glide
            Glide.with(itemView)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(ivProductImage)

            val storeName = product.storeId?.let { storeMap[it]?.storeName } ?: "Unknown Store"
            binding.tvStoreName.text = storeName
            root.setOnClickListener { onClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    fun updateProducts(newProducts: List<ProductsItem>) {
        val diffCallback = ProductDiffCallback(products, newProducts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        products = newProducts
        diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }

    class ProductDiffCallback(
        private val oldList: List<ProductsItem>,
        private val newList: List<ProductsItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id  // Compare unique IDs

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]  // Compare entire object
    }
}