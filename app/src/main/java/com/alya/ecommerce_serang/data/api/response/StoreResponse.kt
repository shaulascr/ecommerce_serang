package com.alya.ecommerce_serang.data.api.response

import com.alya.ecommerce_serang.data.api.dto.Store
import com.google.gson.annotations.SerializedName

data class StoreResponse(

	@field:SerializedName("shipping")
	val shipping: List<ShippingItem>,

	@field:SerializedName("payment")
	val payment: List<PaymentItem>,

	@field:SerializedName("store")
	val store: Store,

	@field:SerializedName("message")
	val message: String
)

data class ShippingItem(

	@field:SerializedName("courier")
	val courier: String
)

data class PaymentItem(

	@field:SerializedName("qris_image")
	val qrisImage: String,

	@field:SerializedName("bank_num")
	val bankNum: String,

	@field:SerializedName("bank_name")
	val bankName: String,

	@field:SerializedName("id")
	val id: Int
)
