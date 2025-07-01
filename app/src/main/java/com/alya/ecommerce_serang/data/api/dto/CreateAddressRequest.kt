package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class CreateAddressRequest (
    @SerializedName("latitude")
    val lat: Double? = null,

    @SerializedName("longitude")
    val long: Double? = null,

    @SerializedName("street")
    val street: String,

    @SerializedName("subdistrict")
    val subDistrict: String,

    @SerializedName("city_id")
    val cityId: String,

    @SerializedName("province_id")
    val provId: Int,
    @SerializedName("postal_code")
    val postCode: String,

    @SerializedName("detail")
    val detailAddress: String? = null,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("recipient")
    val recipient: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("is_store_location")
    val isStoreLocation: Boolean

)