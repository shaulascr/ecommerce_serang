package com.alya.ecommerce_serang.data.api.response.order

import com.google.gson.annotations.SerializedName

data class OrderDetailResponse(

	@field:SerializedName("orders")
	val orders: Orders,

	@field:SerializedName("message")
	val message: String
)

data class Orders(

	@field:SerializedName("receipt_num")
	val receiptNum: String,

	@field:SerializedName("payment_upload_at")
	val paymentUploadAt: String,

	@field:SerializedName("latitude")
	val latitude: String,

	@field:SerializedName("pay_info_name")
	val payInfoName: String,

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("voucher_code")
	val voucherCode: Any,

	@field:SerializedName("updated_at")
	val updatedAt: String,

	@field:SerializedName("etd")
	val etd: String,

	@field:SerializedName("street")
	val street: String,

	@field:SerializedName("cancel_date")
	val cancelDate: String,

	@field:SerializedName("payment_evidence")
	val paymentEvidence: String,

	@field:SerializedName("longitude")
	val longitude: String,

	@field:SerializedName("shipment_status")
	val shipmentStatus: String,

	@field:SerializedName("order_items")
	val orderItems: List<OrderItemsItem>,

	@field:SerializedName("auto_completed_at")
	val autoCompletedAt: String,

	@field:SerializedName("is_store_location")
	val isStoreLocation: Boolean,

	@field:SerializedName("qris_image")
	val qrisImage: String,

	@field:SerializedName("voucher_name")
	val voucherName: Any,

	@field:SerializedName("payment_status")
	val paymentStatus: String,

	@field:SerializedName("address_id")
	val addressId: Int,

	@field:SerializedName("payment_amount")
	val paymentAmount: String,

	@field:SerializedName("cancel_reason")
	val cancelReason: String,

	@field:SerializedName("total_amount")
	val totalAmount: String,

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("province_id")
	val provinceId: Int,

	@field:SerializedName("courier")
	val courier: String,

	@field:SerializedName("subdistrict")
	val subdistrict: String,

	@field:SerializedName("service")
	val service: String,

	@field:SerializedName("pay_info_num")
	val payInfoNum: String,

	@field:SerializedName("shipment_price")
	val shipmentPrice: String,

	@field:SerializedName("voucher_id")
	val voucherId: Any,

	@field:SerializedName("payment_info_id")
	val paymentInfoId: Int,

	@field:SerializedName("detail")
	val detail: String,

	@field:SerializedName("postal_code")
	val postalCode: String,

	@field:SerializedName("order_id")
	val orderId: Int,

	@field:SerializedName("city_id")
	val cityId: Int
)

data class OrderItemsItem(

	@field:SerializedName("order_item_id")
	val orderItemId: Int,

	@field:SerializedName("review_id")
	val reviewId: Int,

	@field:SerializedName("quantity")
	val quantity: Int,

	@field:SerializedName("price")
	val price: Int,

	@field:SerializedName("subtotal")
	val subtotal: Int,

	@field:SerializedName("product_image")
	val productImage: String,

	@field:SerializedName("product_id")
	val productId: Int,

	@field:SerializedName("store_name")
	val storeName: String,

	@field:SerializedName("product_price")
	val productPrice: Int,

	@field:SerializedName("product_name")
	val productName: String
)
