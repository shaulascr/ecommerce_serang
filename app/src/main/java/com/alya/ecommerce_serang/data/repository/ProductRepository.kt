package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import com.alya.ecommerce_serang.data.api.response.ReviewsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val apiService: ApiService) {
    suspend fun getAllProducts(): Result<List<ProductsItem>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("ProductRepository", "Attempting to fetch products")
                val response = apiService.getAllProduct()

                if (response.isSuccessful) {
                    // Return a Result.Success with the list of products

                    Result.Success(response.body()?.products ?: emptyList())

                } else {
                    // Return a Result.Error with a custom Exception
                    Log.e("ProductRepository", "Error: ${response.errorBody()?.string()}")
                    Result.Error(Exception("Failed to fetch products. Code: ${response.code()}"))
                }
            } catch (e: Exception) {
                // Return a Result.Error with the exception caught
                Result.Error(e)
            }
        }

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

    suspend fun getAllCategories(): Result<List<CategoryItem>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("Categories", "Attempting to fetch categories")
                val response = apiService.allCategory()

                if (response.isSuccessful) {
                    val categories = response.body()?.category ?: emptyList()
                    Log.d("Categories", "Fetched categories: $categories")
                    categories.forEach { Log.d("Category Image", "Category: ${it.name}, Image: ${it.image}") }
                    Result.Success(categories)
                } else {
                    Result.Error(Exception("Failed to fetch categories. Code: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("Categories", "Error fetching categories", e)
                Result.Error(e)
            }
        }

    suspend fun fetchProductReview(productId: Int): List<ReviewsItem>? {
        return try {
            val response = apiService.getProductReview(productId)
            if (response.isSuccessful) {
                response.body()?.reviews // Ambil daftar review dari response
            } else {
                Log.e("ProductRepository", "Error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchMyStoreProducts(): List<ProductsItem> {
        val response = apiService.getStoreProduct()
        if (response.isSuccessful) {
            val responseBody = response.body()
            return responseBody?.products?.filterNotNull() ?: emptyList()
        } else {
            throw Exception("Failed to fetch store products: ${response.message()}")
        }
    }
}

//    suspend fun fetchStoreDetail(storeId: Int): Store? {
//        return try {
//            val response = apiService.getStore(storeId)
//            if (response.isSucessful) {
//                response.body()?.store
//            } else {
//                Log.e("ProductRepository", "Error: ${response.errorBody()?.string()}")
//
//                null
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }