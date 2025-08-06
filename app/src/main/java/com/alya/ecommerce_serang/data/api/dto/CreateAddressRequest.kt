package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class CreateAddressRequest(
    @SerializedName("userId")
    val userId: Int,

    @SerializedName("latitude")
    val lat: Double,

    @SerializedName("longitude")
    val long: Double,

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

    @SerializedName("village_id")
    val idVillage: String?, // nullable for now

    @SerializedName("detail")
    val detailAddress: String,

    @SerializedName("is_store_location")
    val isStoreLocation: Boolean,

    @SerializedName("recipient")
    val recipient: String,

    @SerializedName("phone")
    val phone: String
)