package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class UpdateAddressReq(
    @SerializedName("street")
    val street: String? = "",

    @SerializedName("subdistrict")
    val subdistrict: String? = "",

    @SerializedName("postal_code")
    val postalCCode: String? = "",

    @SerializedName("detail")
    val detail: String? = "",

    @SerializedName("city_id")
    val cityId: String? = "",

    @SerializedName("province_id")
    val provId: String? = "",

    @SerializedName("phone")
    val phone: String? = ""


)