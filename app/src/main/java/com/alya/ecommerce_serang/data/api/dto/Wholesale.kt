package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class Wholesale(

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("wholesale_price")
    val wholesalePrice: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("min_item")
    val minItem: Int? = null
)
