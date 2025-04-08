package com.alya.ecommerce_serang.data.api.response

import com.google.gson.annotations.SerializedName

data class DetailStoreProductResponse(

	@field:SerializedName("store")
	val store: StoreProduct,

	@field:SerializedName("message")
	val message: String
)

data class StoreProduct(

	@field:SerializedName("store_name")
	val storeName: String,

	@field:SerializedName("description")
	val description: String,

	@field:SerializedName("store_type")
	val storeType: String,

	@field:SerializedName("store_location")
	val storeLocation: String,

	@field:SerializedName("store_image")
	val storeImage: String? = null,

	@field:SerializedName("status")
	val status: String
)
