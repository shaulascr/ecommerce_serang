package com.alya.ecommerce_serang.ui.profile.mystore.review

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ReviewsItem
import com.alya.ecommerce_serang.utils.viewmodel.ReviewViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReviewAdapter(
    private val viewModel: ReviewViewModel
): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private val reviews = mutableListOf<ReviewsItem>()
    private var fragmentScore: String = "all"

    fun setFragmentScore(score: String) {
        fragmentScore = score
    }

    fun submitList(newReviews: List<ReviewsItem>) {
        reviews.clear()
        reviews.addAll(newReviews)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReviewAdapter.ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store_product_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        if (position < reviews.size) {
            holder.bind(reviews[position])
        } else {
            Log.e("ReviewAdapter", "Position $position is out of bounds for size ${reviews.size}")
        }
    }

    override fun getItemCount(): Int = reviews.size

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.iv_product)
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvReviewScore: TextView = itemView.findViewById(R.id.tv_review_score)
        private val tvReviewDate: TextView = itemView.findViewById(R.id.tv_review_date)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvReviewDesc: TextView = itemView.findViewById(R.id.tv_review_desc)
        private val ivMenu: ImageView = itemView.findViewById(R.id.iv_menu)

        fun bind(review: ReviewsItem) {
            val actualScore =
                if (fragmentScore == "all") review.rating.toString() else fragmentScore

            CoroutineScope(Dispatchers.Main).launch {
                val imageUrl = viewModel.getProductImage(review.productId ?: -1)
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .into(ivProduct)
            }

            tvProductName.text = review.productName
            tvReviewScore.text = actualScore
            tvReviewDate.text = review.reviewDate
            tvUsername.text = review.username
            tvReviewDesc.text = review.reviewText
        }
    }
}