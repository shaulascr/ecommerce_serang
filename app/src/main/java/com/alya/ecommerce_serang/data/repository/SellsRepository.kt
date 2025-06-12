package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.ConfirmPaymentRequest
import com.alya.ecommerce_serang.data.api.dto.ConfirmShipmentRequest
import com.alya.ecommerce_serang.data.api.dto.PaymentConfirmRequest
import com.alya.ecommerce_serang.data.api.response.store.GenericResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.OrderDetailResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.OrderListResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.PaymentConfirmationResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import retrofit2.Response

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

    suspend fun getSellDetails(orderId: Int): OrderDetailResponse? {
        return try {
            val response = apiService.getSellDetail(orderId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Log.e("SellsRepository", "Error getting order details", e)
            null
        }
    }

    suspend fun confirmPayment(request: ConfirmPaymentRequest): Response<GenericResponse> {
        return try {
            Log.d("SellsRepository", "Calling confirmPayment with orderId=${request.orderId}, status=${request.status}")
            apiService.confirmPayment(request)
        } catch (e: Exception) {
            Log.e("SellsRepository", "Error during confirmPayment", e)
            throw e
        }
    }

    suspend fun confirmShipment(request: ConfirmShipmentRequest): Response<GenericResponse> {
        return try {
            Log.d("SellsRepository", "Calling confirmShipment with orderId=${request.orderId}, receipt num=${request.receiptNum}")
            apiService.confirmShipment(request)
        } catch (e: Exception) {
            throw e
        }
    }

}