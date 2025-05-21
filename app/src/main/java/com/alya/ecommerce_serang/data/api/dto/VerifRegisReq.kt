package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class VerifRegisReq (
    @SerializedName("field")
    val fieldRegis: String,

    @SerializedName("value")
    val valueRegis: String
)