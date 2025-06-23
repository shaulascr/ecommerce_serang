package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class ReviewsItem(

    @field:SerializedName("order_item_id")
    val orderItemId: Int? = null,

    @field:SerializedName("review_date")
    val reviewDate: String? = null,

    @field:SerializedName("user_image")
    val userImage: String? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("rating")
    val rating: Int? = null,

    @field:SerializedName("review_text")
    val reviewText: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("username")
    val username: String? = null
)