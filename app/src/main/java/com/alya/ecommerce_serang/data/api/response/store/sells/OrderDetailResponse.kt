package com.alya.ecommerce_serang.data.api.response.store.sells

import com.google.gson.annotations.SerializedName

data class OrderDetailResponse(

	@field:SerializedName("orders")
	val orders: Orders? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class Orders(

	@field:SerializedName("receipt_num")
	val receiptNum: Any? = null,

	@field:SerializedName("payment_upload_at")
	val paymentUploadAt: Any? = null,

	@field:SerializedName("latitude")
	val latitude: Any? = null,

	@field:SerializedName("pay_info_name")
	val payInfoName: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("voucher_code")
	val voucherCode: Any? = null,

	@field:SerializedName("order_status")
	val orderStatus: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("etd")
	val etd: String? = null,

	@field:SerializedName("street")
	val street: String? = null,

	@field:SerializedName("cancel_date")
	val cancelDate: Any? = null,

	@field:SerializedName("payment_evidence")
	val paymentEvidence: Any? = null,

	@field:SerializedName("longitude")
	val longitude: Any? = null,

	@field:SerializedName("shipment_status")
	val shipmentStatus: String? = null,

	@field:SerializedName("order_items")
	val orderItems: List<OrderItemsItem?>? = null,

	@field:SerializedName("auto_completed_at")
	val autoCompletedAt: Any? = null,

	@field:SerializedName("is_store_location")
	val isStoreLocation: Boolean? = null,

	@field:SerializedName("qris_image")
	val qrisImage: String? = null,

	@field:SerializedName("voucher_name")
	val voucherName: Any? = null,

	@field:SerializedName("payment_status")
	val paymentStatus: Any? = null,

	@field:SerializedName("address_id")
	val addressId: Int? = null,

	@field:SerializedName("payment_amount")
	val paymentAmount: Any? = null,

	@field:SerializedName("cancel_reason")
	val cancelReason: Any? = null,

	@field:SerializedName("total_amount")
	val totalAmount: String? = null,

	@field:SerializedName("user_id")
	val userId: Int? = null,

	@field:SerializedName("province_id")
	val provinceId: Int? = null,

	@field:SerializedName("courier")
	val courier: String? = null,

	@field:SerializedName("subdistrict")
	val subdistrict: String? = null,

	@field:SerializedName("service")
	val service: String? = null,

	@field:SerializedName("pay_info_num")
	val payInfoNum: String? = null,

	@field:SerializedName("shipment_price")
	val shipmentPrice: String? = null,

	@field:SerializedName("voucher_id")
	val voucherId: Any? = null,

	@field:SerializedName("payment_info_id")
	val paymentInfoId: Int? = null,

	@field:SerializedName("detail")
	val detail: String? = null,

	@field:SerializedName("postal_code")
	val postalCode: String? = null,

	@field:SerializedName("order_id")
	val orderId: Int? = null,

	@field:SerializedName("username")
	val username: String? = null,

	@field:SerializedName("city_id")
	val cityId: Int? = null
)
