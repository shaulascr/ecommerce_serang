package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.response.store.orders.OrderListResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService

class SellsRepository(private val apiService: ApiService) {
    suspend fun getSellList(status: String): Result<OrderListResponse> {
        return try {
            Log.d("SellsRepository", "Add Evidence : $status")
            val response = apiService.getSellList(status)

            if (response.isSuccessful) {
                val allListSell = response.body()
                if (allListSell != null) {
                    Log.d("SellsRepository", "Add Evidence successfully: ${allListSell.message}")
                    Result.Success(allListSell)
                } else {
                    Log.e("SellsRepository", "Response body was null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("SellsRepository", "Error Add Evidence : $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("SellsRepository", "Exception Add Evidence ", e)
            Result.Error(e)
        }
    }

    suspend fun updateOrderStatus(orderId: Int?, status: String) {
        try {
            val response = apiService.updateOrder(orderId, status)
            if (response.isSuccessful) {
                Log.d("SellsRepository", "Order status updated successfully: orderId=$orderId, status=$status")
            } else {
                Log.e("SellsRepository", "Error updating order status: orderId=$orderId, status=$status")
            }
        } catch (e: Exception) {
            Log.e("SellsRepository", "Exception updating order status", e)
        }
    }
}