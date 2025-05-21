package com.alya.ecommerce_serang.data.api.response.customer.product

import com.google.gson.annotations.SerializedName

data class DetailStoreProductResponse(

	@field:SerializedName("shipping")
	val shipping: List<ShippingItemDetail>,

	@field:SerializedName("payment")
	val payment: List<PaymentItemDetail>,

	@field:SerializedName("store")
	val store: StoreProduct,

	@field:SerializedName("message")
	val message: String
)

data class StoreProduct(

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
	val storeImage: String? = null,

	@field:SerializedName("status")
	val status: String
)

data class ShippingItemDetail(

	@field:SerializedName("courier")
	val courier: String
)

data class PaymentItemDetail(

	@field:SerializedName("qris_image")
	val qrisImage: String,

	@field:SerializedName("bank_num")
	val bankNum: String,

	@field:SerializedName("account_name")
	val accountName: Any,

	@field:SerializedName("bank_name")
	val bankName: String,

	@field:SerializedName("id")
	val id: Int
)
