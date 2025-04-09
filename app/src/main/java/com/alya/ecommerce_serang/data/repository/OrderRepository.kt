package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.response.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.profile.AddressResponse
import com.alya.ecommerce_serang.data.api.response.profile.CreateAddressResponse
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

    suspend fun createOrder(orderRequest: OrderRequest): Response<CreateOrderResponse> {
        return apiService.postOrder(orderRequest)
    }

    suspend fun getStore(): StoreResponse? {
        val response = apiService.getStore()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getAddress(): AddressResponse?{
        val response = apiService.getAddress()
        return if (response.isSuccessful) response.body() else null
    }

    //post data with message/response
    suspend fun addAddress(createAddressRequest: CreateAddressRequest): Result<CreateAddressResponse> {
        return try {
            val response = apiService.createAddress(createAddressRequest)
            if (response.isSuccessful){
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Add Address failed"))
            } else {
                Log.e("OrderRepository", "Error: ${response.errorBody()?.string()}")
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getListProvinces(): ListProvinceResponse? {
        val response = apiService.getListProv()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getListCities(provId : Int): ListCityResponse?{
        val response = apiService.getCityProvId(provId)
        return if (response.isSuccessful) response.body() else null
    }

}