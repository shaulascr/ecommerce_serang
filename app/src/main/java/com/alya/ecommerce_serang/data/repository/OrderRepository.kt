package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.response.OrderResponse
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import com.alya.ecommerce_serang.data.api.response.StoreResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import retrofit2.Response

class OrderRepository(private val apiService: ApiService) {

    suspend fun fetchProductDetail(productId: Int): ProductResponse? {
        return try {
            val response = apiService.getDetailProduct(productId)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("ProductRepository", "Error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createOrder(orderRequest: OrderRequest): Response<OrderResponse> {
        return apiService.postOrder(orderRequest)
    }

    suspend fun getStore(): StoreResponse? {
        val response = apiService.getStore()
        return if (response.isSuccessful) response.body() else null
    }


    //not yet implement the api service address
//    suspend fun getAddressDetails(addressId: Int): AddressesItem {
//        // Simulate API call to get address details
//        kotlinx.coroutines.delay(300) // Simulate network request
//        // Return mock data
//        return AddressesItem(
//            id = addressId,
//            label = "Rumah",
//            fullAddress = "Jl. Pegangasan Timur No. 42, Jakarta"
//        )
//    }

}