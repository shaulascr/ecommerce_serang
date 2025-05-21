package com.alya.ecommerce_serang.data.api.response.customer.order

import com.google.gson.annotations.SerializedName

data class CreateReviewResponse(

	@field:SerializedName("order_item_id")
	val orderItemId: Int,

	@field:SerializedName("rating")
	val rating: Int,

	@field:SerializedName("review_text")
	val reviewText: String
)
