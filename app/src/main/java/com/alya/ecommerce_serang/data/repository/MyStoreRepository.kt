package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.OrderListResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MyStoreRepository(private val apiService: ApiService) {
    suspend fun fetchMyStoreProfile(): Result<Store?> {
        return try {
            val response = apiService.getStore()

            if (response.isSuccessful) {
                val storeResponse: StoreResponse? = response.body()
                Result.Success(storeResponse?.store)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown API error"
                Log.e("MyStoreRepository", "Error: $errorMessage")
                Result.Error(HttpException(response))
            }
        } catch (e: IOException) {
            Log.e("MyStoreRepository", "Network error: ${e.message}")
            Result.Error(e)
        } catch (e: Exception) {
            Log.e("MyStoreRepository", "Unexpected error: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun listStoreType(): Result<ListStoreTypeResponse>{
        return try{
            val response = apiService.listTypeStore()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("No store type"))
            } else {
                throw Exception("No response ${response.errorBody()?.string()}")
            }
        } catch (e:Exception){
            Result.Error(e)
        }
    }

    suspend fun updateStoreProfile(
        storeName: RequestBody,
        storeStatus: RequestBody,
        storeDescription: RequestBody,
        isOnLeave: RequestBody,
        cityId: RequestBody,
        provinceId: RequestBody,
        street: RequestBody,
        subdistrict: RequestBody,
        detail: RequestBody,
        postalCode: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        userPhone: RequestBody,
        storeType: RequestBody,
        storeimg: MultipartBody.Part?
    ): Response<StoreDataResponse> {
        return apiService.updateStoreProfileMultipart(
            storeName, storeStatus, storeDescription, isOnLeave, cityId, provinceId,
            street, subdistrict, detail, postalCode, latitude, longitude, userPhone, storeType, storeimg
        )
    }

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

    suspend fun getBalance(): Result<com.alya.ecommerce_serang.data.api.response.store.StoreResponse> {
        return try {
            val response = apiService.getMyStoreData()

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.Error(IllegalStateException("Response body is null"))

                // Validate the balance field
                val balanceRaw = body.store.balance
                balanceRaw.toDoubleOrNull()
                    ?: return Result.Error(NumberFormatException("Invalid balance format: $balanceRaw"))

                Result.Success(body)
            } else {
                Result.Error(
                    Exception("Failed to load balance: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Log.e("MyStoreRepository", "Error fetching balance", e)
            Result.Error(e)
        }
    }

//    private fun fetchBalance() {
//        showLoading(true)
//        lifecycleScope.launch {
//            try {
//                val response = ApiConfig.getApiService(sessionManager).getMyStoreData()
//                if (response.isSuccessful && response.body() != null) {
//                    val storeData = response.body()!!
//                    val balance = storeData.store.balance
//
//                    // Format the balance
//                    try {
//                        val balanceValue = balance.toDouble()
//                        binding.tvBalance.text = String.format("Rp%,.0f", balanceValue)
//                    } catch (e: Exception) {
//                        binding.tvBalance.text = "Rp$balance"
//                    }
//                } else {
//                    Toast.makeText(
//                        this@BalanceActivity,
//                        "Gagal memuat data saldo: ${response.message()}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error fetching balance", e)
//                Toast.makeText(
//                    this@BalanceActivity,
//                    "Error: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
}