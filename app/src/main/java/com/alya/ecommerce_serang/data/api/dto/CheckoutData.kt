package com.alya.ecommerce_serang.data.api.dto

data class CheckoutData(
    val orderRequest: OrderRequest,
    // Additional UI-related data
    val productName: String,
    val productImageUrl: String,
    val productPrice: Double,
    val sellerName: String,
    val sellerImageUrl: String,
    val sellerId: Int
)