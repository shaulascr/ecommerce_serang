package com.alya.ecommerce_serang.data.api.response.store.review

import com.alya.ecommerce_serang.data.api.dto.ReviewsItem
import com.google.gson.annotations.SerializedName

data class ProductReviewResponse(

	@field:SerializedName("reviews")
	val reviews: List<ReviewsItem?>? = null,

	@field:SerializedName("message")
	val message: String? = null
)
