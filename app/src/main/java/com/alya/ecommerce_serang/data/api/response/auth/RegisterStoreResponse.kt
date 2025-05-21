package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class RegisterStoreResponse(

    @field:SerializedName("store")
	val store: Store,

    @field:SerializedName("message")
	val message: String
)

data class Store(

	@field:SerializedName("image")
	val image: String,

	@field:SerializedName("ktp")
	val ktp: String,

	@field:SerializedName("nib")
	val nib: String,

	@field:SerializedName("npwp")
	val npwp: String,

	@field:SerializedName("address_id")
	val addressId: Int,

	@field:SerializedName("description")
	val description: String,

	@field:SerializedName("store_type_id")
	val storeTypeId: Int,

	@field:SerializedName("is_on_leave")
	val isOnLeave: Boolean,

	@field:SerializedName("balance")
	val balance: String,

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("name")
	val name: String,

	@field:SerializedName("persetujuan")
	val persetujuan: String,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("status")
	val status: String
)
