package com.alya.ecommerce_serang.data.api.response.customer.order

data class CancelOrderResponse(
	val data: DataCancel,
	val message: String
)

data class DataCancel(
	val reason: String,
	val createdAt: String,
	val id: Int,
	val orderId: Int
)

