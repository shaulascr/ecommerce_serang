package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.cart.AddCartResponse
import com.alya.ecommerce_serang.data.api.response.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.response.product.StoreProduct
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

                    val products = response.body()?.products ?: emptyList()
                    Log.d(TAG, "Products fetched successfully. Total products: ${products.size}")

                    // Optional: Log some product details
                    products.take(3).forEach { product ->
                        Log.d(TAG, "Sample Product - ID: ${product.id}, Name: ${product.name}, Price: ${product.price}")
                    }

                    Result.Success(products)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to fetch products. Code: ${response.code()}, Error: $errorBody")
                    Result.Error(Exception("Failed to fetch products. Code: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching products", e)
                Result.Error(e)
            }
        }

    suspend fun fetchProductDetail(productId: Int): ProductResponse? {
        return try {
            Log.d(TAG, "Fetching product detail for ID: $productId")
            val response = apiService.getDetailProduct(productId)
            if (response.isSuccessful) {
                val productResponse = response.body()
                Log.d(TAG, "Product detail fetched successfully. Product: ${productResponse?.product?.productName}")
                productResponse
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching product detail. Code: ${response.code()}, Error: $errorBody")
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
                    Log.d("ProductRepository", "Fetched categories: $categories")
                    categories.forEach { Log.d("Category Image", "Category: ${it.name}, Image: ${it.image}") }
                    Result.Success(categories)
                } else {
                    Result.Error(Exception("Failed to fetch categories. Code: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching categories", e)
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

    suspend fun addToCart(request: CartItem): Result<AddCartResponse> {
        return try {
            val response = apiService.addCart(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Add Cart failed"))
            } else {
                Log.e("ProductRepository", "Error: ${response.errorBody()?.string()}")
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown Error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchStoreDetail(storeId: Int): Result<StoreProduct?> {
        return try {
            val response = apiService.getDetailStore(storeId)
            if (response.isSuccessful) {
                val store = response.body()?.store
                if (store != null) {
                    Result.Success(store)
                } else {
                    Result.Error(Throwable("Empty response body"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ProductRepository", "Error: $errorMsg")
                Result.Error(Throwable(errorMsg))
            }
        } catch (e: Exception) {
            Result.Error(e)
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

    suspend fun addProduct(
        name: String,
        description: String,
        price: Int,
        stock: Int,
        minOrder: Int,
        weight: Int,
        isPreOrder: Boolean,
        duration: Int,
        categoryId: Int,
        isActive: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val status = if (isActive) "active" else "inactive"
            val response = apiService.addProduct(
                name = name,
                description = description,
                price = price,
                stock = stock,
                minOrder = minOrder,
                weight = weight,
                isPreOrder = isPreOrder,
                duration = duration,
                categoryId = categoryId,
                isActive = status
            )

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to add product. Code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    companion object {
        private const val TAG = "ProductRepository"
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