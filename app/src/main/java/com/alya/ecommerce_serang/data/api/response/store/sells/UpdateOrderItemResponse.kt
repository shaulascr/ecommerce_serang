package com.alya.ecommerce_serang.data.api.response.store.sells

import com.google.gson.annotations.SerializedName

data class UpdateOrderItemResponse(

	@field:SerializedName("total_amount")
	val totalAmount: Int? = null,

	@field:SerializedName("order_id")
	val orderId: Int? = null,

	@field:SerializedName("items")
	val items: List<ItemsItem?>? = null,

	@field:SerializedName("status")
	val status: String? = null
)

data class ItemsItem(

	@field:SerializedName("order_item_id")
	val orderItemId: Int? = null,

	@field:SerializedName("price")
	val price: Int? = null,

	@field:SerializedName("subtotal")
	val subtotal: Int? = null
)
