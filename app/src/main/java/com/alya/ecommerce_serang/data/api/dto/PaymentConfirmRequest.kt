package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class PaymentConfirmRequest (
    @SerializedName("order_id")
    val orderId : Int,

    @SerializedName("status")
    val status: String

)