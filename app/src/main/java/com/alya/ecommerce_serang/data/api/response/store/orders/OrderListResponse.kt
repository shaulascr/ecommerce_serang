package com.alya.ecommerce_serang.data.api.response.store.orders

import com.google.gson.annotations.SerializedName

data class OrderListResponse(

	@field:SerializedName("orders")
	val orders: List<OrdersItem?>? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class Voucher(

	@field:SerializedName("name")
	val name: Any? = null,

	@field:SerializedName("voucher_id")
	val voucherId: Any? = null,

	@field:SerializedName("voucher_code")
	val voucherCode: Any? = null
)

data class Shipment(

	@field:SerializedName("receipt_num")
	val receiptNum: Any? = null,

	@field:SerializedName("courier")
	val courier: Any? = null,

	@field:SerializedName("price")
	val price: Any? = null,

	@field:SerializedName("service")
	val service: Any? = null,

	@field:SerializedName("shipment_id")
	val shipmentId: Any? = null,

	@field:SerializedName("status")
	val status: Any? = null
)

data class OrderItemsItem(

	@field:SerializedName("review_id")
	val reviewId: Int? = null,

	@field:SerializedName("quantity")
	val quantity: Int? = null,

	@field:SerializedName("price")
	val price: Int? = null,

	@field:SerializedName("subtotal")
	val subtotal: Int? = null,

	@field:SerializedName("product_image")
	val productImage: String? = null,

	@field:SerializedName("store_name")
	val storeName: String? = null,

	@field:SerializedName("product_price")
	val productPrice: Int? = null,

	@field:SerializedName("product_name")
	val productName: String? = null
)

data class Address(

	@field:SerializedName("is_store_location")
	val isStoreLocation: Boolean? = null,

	@field:SerializedName("province_id")
	val provinceId: Int? = null,

	@field:SerializedName("street")
	val street: String? = null,

	@field:SerializedName("subdistrict")
	val subdistrict: String? = null,

	@field:SerializedName("latitude")
	val latitude: Any? = null,

	@field:SerializedName("address_id")
	val addressId: Int? = null,

	@field:SerializedName("detail")
	val detail: String? = null,

	@field:SerializedName("postal_code")
	val postalCode: String? = null,

	@field:SerializedName("longitude")
	val longitude: Any? = null,

	@field:SerializedName("city_id")
	val cityId: Int? = null
)

data class Payment(

	@field:SerializedName("evidence")
	val evidence: Any? = null,

	@field:SerializedName("uploaded_at")
	val uploadedAt: Any? = null,

	@field:SerializedName("payment_id")
	val paymentId: Any? = null
)

data class OrdersItem(

	@field:SerializedName("address")
	val address: Address? = null,

	@field:SerializedName("shipment")
	val shipment: Shipment? = null,

	@field:SerializedName("voucher")
	val voucher: Voucher? = null,

	@field:SerializedName("address_id")
	val addressId: Int? = null,

	@field:SerializedName("is_negotiable")
	val isNegotiable: Boolean? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("payment_method_id")
	val paymentMethodId: Int? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("user_id")
	val userId: Int? = null,

	@field:SerializedName("total_amount")
	val totalAmount: String? = null,

	@field:SerializedName("voucher_id")
	val voucherId: Any? = null,

	@field:SerializedName("payment")
	val payment: Payment? = null,

	@field:SerializedName("order_id")
	val orderId: Int? = null,

	@field:SerializedName("username")
	val username: String? = null,

	@field:SerializedName("status")
	val status: String? = null,

	@field:SerializedName("order_items")
	val orderItems: List<OrderItemsItem?>? = null
)
