package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class ReviewProductItem (
    @SerializedName("order_item_id")
    val orderItemId : Int,

    @SerializedName("rating")
    val rating : Int,

    @SerializedName("review_text")
    val reviewTxt : String = ""
)

data class ReviewUIItem(
    val orderItemId: Int,
    val productName: String,
    val productImage: String,
    var rating: Int = 5,  // Default rating is 5 stars
    var reviewText: String = ""  // Empty by default, to be filled by user
)