package com.alya.ecommerce_serang.data.api.response.customer.profile

import com.google.gson.annotations.SerializedName

data class UpdateAddressResponse(

	@field:SerializedName("address")
	val address: Address,

	@field:SerializedName("message")
	val message: String
)

data class Address(

	@field:SerializedName("village_id")
	val villageId: String,

	@field:SerializedName("is_store_location")
	val isStoreLocation: Boolean,

	@field:SerializedName("latitude")
	val latitude: String,

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("province_id")
	val provinceId: String,

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
	val cityId: String
)
