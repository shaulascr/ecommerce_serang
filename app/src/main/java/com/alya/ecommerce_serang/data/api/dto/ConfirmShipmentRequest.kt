package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class ConfirmShipmentRequest(
    @SerializedName("receipt_num")
    val receiptNum: String,

    @SerializedName("order_id")
    val orderId: Int
)
