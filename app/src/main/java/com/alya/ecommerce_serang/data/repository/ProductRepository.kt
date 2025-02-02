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
                    Result.success(response.body()?.products ?: emptyList())
                } else {
                    Result.failure(Exception("Failed to fetch products"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
//    suspend fun getCategories():List<Category>
//
//    fun getProducts(query: ProductQuery) : Flow<PagingData<Product>>
//    fun getRecentSearchs(): Flow<List<String>>
//    suspend fun clearRecents()
//    suspend fun addRecents(search:String)
//    suspend fun getProduct(id:String):DetailProduct
}