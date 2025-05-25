package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
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
}