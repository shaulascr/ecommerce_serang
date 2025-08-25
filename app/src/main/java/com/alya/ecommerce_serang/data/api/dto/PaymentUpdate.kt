package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName
import java.io.File

data class PaymentUpdate(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("bank_name")
    val bankName: String,

    @field:SerializedName("bank_num")
    val bankNum: String,

    @field:SerializedName("account_name")
    val accountName: String,

    @field:SerializedName("qris_image")
    val qrisImage: File? = null
)
