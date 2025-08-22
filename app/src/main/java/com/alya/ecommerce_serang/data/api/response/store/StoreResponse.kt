package com.alya.ecommerce_serang.data.api.response.store

import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.store.profile.Payment
import com.alya.ecommerce_serang.data.api.response.store.profile.Shipping
import com.google.gson.annotations.SerializedName

data class StoreResponse(
    val message: String,
    val store: Store,
    val shipping: List<Shipping> = emptyList(),
    val payment: List<Payment> = emptyList()
)