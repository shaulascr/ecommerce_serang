package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class CancelOrderReq (
    @SerializedName("order_id")
    val orderId: Int,

    @SerializedName("reason")
    val reason: String
)