package com.alya.ecommerce_serang.data.api.response

import com.google.gson.annotations.SerializedName

data class AddressResponse(

	@field:SerializedName("addresses")
	val addresses: List<AddressesItem>,

	@field:SerializedName("message")
	val message: String
)

data class AddressesItem(

	@field:SerializedName("is_store_location")
	val isStoreLocation: Boolean,

	@field:SerializedName("latitude")
	val latitude: String,

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("province_id")
	val provinceId: Int,

	@field:SerializedName("phone")
	val phone: Any,

	@field:SerializedName("street")
	val street: String,

	@field:SerializedName("subdistrict")
	val subdistrict: String,

	@field:SerializedName("recipient")
	val recipient: Any,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("detail")
	val detail: String,

	@field:SerializedName("postal_code")
	val postalCode: String,

	@field:SerializedName("longitude")
	val longitude: String,

	@field:SerializedName("city_id")
	val cityId: Int
)
