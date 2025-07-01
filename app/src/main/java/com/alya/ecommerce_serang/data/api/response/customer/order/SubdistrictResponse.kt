package com.alya.ecommerce_serang.data.api.response.customer.order

import com.google.gson.annotations.SerializedName

data class SubdistrictResponse(

	@field:SerializedName("subdistricts")
	val subdistricts: List<SubdistrictsItem>,

	@field:SerializedName("message")
	val message: String
)

data class SubdistrictsItem(

	@field:SerializedName("subdistrict_id")
	val subdistrictId: String,

	@field:SerializedName("subdistrict_name")
	val subdistrictName: String
)
