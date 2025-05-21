package com.alya.ecommerce_serang.ui.order.review

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ReviewUIItem
import com.alya.ecommerce_serang.databinding.ItemReviewProductBinding
import com.bumptech.glide.Glide

class AddReviewAdapter(
    private val items: List<ReviewUIItem>,
    private val onRatingChanged: (position: Int, rating: Int) -> Unit,
    private val onReviewTextChanged: (position: Int, text: String) -> Unit
) : RecyclerView.Adapter<AddReviewAdapter.AddReviewViewHolder>() {

    inner class AddReviewViewHolder(private val binding: ItemReviewProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReviewUIItem) {
            binding.apply {
                tvProductName.text = item.productName

                Glide.with(itemView.context)
                    .load(item.productImage)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivProduct)

                ratingBar.rating = item.rating.toFloat()
                etReviewText.setText(item.reviewText)

                ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    onRatingChanged(adapterPosition, rating.toInt())
                }

                etReviewText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(editable: Editable?) {
                        onReviewTextChanged(adapterPosition, editable.toString())
                    }
                })

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddReviewViewHolder {
        val binding = ItemReviewProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}