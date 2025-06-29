package com.alya.ecommerce_serang.ui.product.listproduct

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.databinding.ItemCategoryHomeBinding
import com.bumptech.glide.Glide

class ListCategoryAdapter(
    private var categories:List<CategoryItem>,
    private val onClick:(category: CategoryItem) -> Unit
): RecyclerView.Adapter<ListCategoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCategoryHomeBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(category: CategoryItem) = with(binding) {
            Log.d("CategoriesAdapter", "Binding category: ${category.name}, Image: ${category.image}")

            val fullImageUrl = if (category.image.startsWith("/")) {
                BASE_URL + category.image.removePrefix("/") // Append base URL if the path starts with "/"
            } else {
                category.image // Use as is if it's already a full URL
            }

            Log.d("CategoriesAdapter", "Loading image: $fullImageUrl")

            Glide.with(itemView.context)
                .load(fullImageUrl)  // Ensure full URL
                .placeholder(R.drawable.placeholder_image)
                .into(imageCategory)

            name.text = category.name

            root.setOnClickListener { onClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder (
        ItemCategoryHomeBinding.inflate(LayoutInflater.from(parent.context),parent,false)
    )

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: ListCategoryAdapter.ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    fun updateData(newCategories: List<CategoryItem>) {
        categories = newCategories.toList()
        notifyDataSetChanged()
    }
}