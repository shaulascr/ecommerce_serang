package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class OrderRequest (
    @SerializedName("address_id")
    val addressId : Int,

    @SerializedName("payment_info_id")
    val paymentMethodId : Int,

    @SerializedName("ship_price")
    val shipPrice : Int,

    @SerializedName("ship_name")
    val shipName : String,

    @SerializedName("ship_service")
    val shipService : String,

    @SerializedName("is_negotiable")
    val isNego: Boolean,

    @SerializedName("cart_item_ids")
    val cartItemId: List<Int>,

    @SerializedName("ship_etd")
    val shipEtd: String
    )