package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class ListStoreTypeResponse(

	@field:SerializedName("storeTypes")
	val storeTypes: List<StoreTypesItem>,

	@field:SerializedName("message")
	val message: String
)

data class StoreTypesItem(

	@field:SerializedName("name")
	val name: String,

	@field:SerializedName("id")
	val id: Int
)
