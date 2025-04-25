package com.alya.ecommerce_serang.data.api.dto

import com.alya.ecommerce_serang.data.api.response.store.orders.Address
import com.alya.ecommerce_serang.data.api.response.store.orders.OrderItemsItem
import com.alya.ecommerce_serang.data.api.response.store.orders.Payment
import com.alya.ecommerce_serang.data.api.response.store.orders.Shipment
import com.alya.ecommerce_serang.data.api.response.store.orders.Voucher
import com.google.gson.annotations.SerializedName

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
