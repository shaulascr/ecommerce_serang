package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class ReviewProductItem (
    @SerializedName("order_item_id")
    val orderItemId : Int,

    @SerializedName("rating")
    val rating : Int,

    @SerializedName("review_text")
    val reviewTxt : String
)