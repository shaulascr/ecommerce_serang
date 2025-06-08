package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class OrderItemsItem(

    @field:SerializedName("order_item_id")
    val orderItemId: Int? = null,

    @field:SerializedName("review_id")
    val reviewId: Int? = null,

    @field:SerializedName("quantity")
    val quantity: Int? = null,

    @field:SerializedName("price")
    val price: Int? = null,

    @field:SerializedName("subtotal")
    val subtotal: Int? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("store_name")
    val storeName: String? = null,

    @field:SerializedName("product_price")
    val productPrice: Int? = null,

    @field:SerializedName("product_name")
    val productName: String? = null
)
