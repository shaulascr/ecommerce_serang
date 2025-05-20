package com.alya.ecommerce_serang.data.api.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RegisterRequest (
    val name: String?,
    val email: String?,
    val password: String?,
    val username: String?,
    val phone: String?,
    @SerializedName("birth_date")
    val birthDate: String?,

    @SerializedName("userimg")
    val image: String? = null,

    val otp: String? = null
): Parcelable