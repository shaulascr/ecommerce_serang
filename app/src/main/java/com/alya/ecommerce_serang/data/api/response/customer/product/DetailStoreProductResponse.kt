package com.alya.ecommerce_serang.data.api.response.customer.product

import com.google.gson.annotations.SerializedName

data class DetailStoreProductResponse(

	@field:SerializedName("shipping")
	val shipping: List<DetailShippingItem>,

	@field:SerializedName("payment")
	val payment: List<DetailPaymentItem>,

	@field:SerializedName("store")
	val store: List<StoreItem>,

	@field:SerializedName("message")
	val message: String
)

data class StoreItem(

	@field:SerializedName("store_id")
	val storeId: Int,

	@field:SerializedName("store_rating")
	val storeRating: String,

	@field:SerializedName("store_name")
	val storeName: String,

	@field:SerializedName("description")
	val description: String,

	@field:SerializedName("store_type")
	val storeType: String,

	@field:SerializedName("store_location")
	val storeLocation: String,

	@field:SerializedName("store_image")
	val storeImage: String,

	@field:SerializedName("status")
	val status: String
)

data class DetailShippingItem(

	@field:SerializedName("courier")
	val courier: String
)

data class DetailPaymentItem(

	@field:SerializedName("qris_image")
	val qrisImage: String,

	@field:SerializedName("bank_num")
	val bankNum: String,

	@field:SerializedName("account_name")
	val accountName: String,

	@field:SerializedName("bank_name")
	val bankName: String,

	@field:SerializedName("id")
	val id: Int
)
