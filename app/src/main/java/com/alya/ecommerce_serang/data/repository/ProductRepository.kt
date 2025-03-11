package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.response.ProductsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val apiService: ApiService) {
    suspend fun getAllProducts(): Result<List<ProductsItem>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("ProductRepository", "Attempting to fetch products")
                val response = apiService.getAllProduct().execute()
                Log.d("ProductRepository", "Response received. Success: ${response.isSuccessful}")
                Log.d("ProductRepository", "Response code: ${response.code()}")
                Log.d("ProductRepository", "Response message: ${response.message()}")

                if (response.isSuccessful) {
                    // Return a Result.Success with the list of products
                    Result.Success(response.body()?.products ?: emptyList())
                } else {
                    // Return a Result.Error with a custom Exception
                    Result.Error(Exception("Failed to fetch products. Code: ${response.code()}"))
                }
            } catch (e: Exception) {
                // Return a Result.Error with the exception caught
                Result.Error(e)
            }
        }
}