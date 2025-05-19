package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class FcmReq (
    @SerializedName("fcm_req")
    val fcmToken: String?= null
)