package com.alya.ecommerce_serang.data.api.response.store

import com.google.gson.annotations.SerializedName

data class GenericResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("success")
    val success: Boolean = true
)