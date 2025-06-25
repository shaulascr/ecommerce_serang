package com.alya.ecommerce_serang.data.repository

import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.store.review.ProductReviewResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService

class ReviewRepository(private val apiService: ApiService) {
    suspend fun getReviewList(score: String): Result<ProductReviewResponse> {
        return try {
            val response = apiService.getStoreProductReview()

            if (response.isSuccessful) {
                val allReviews = response.body()
                val filteredReviews = if (score == "all") {
                    allReviews
                } else {
                    val targetScore = score.toIntOrNull()
                    allReviews?.copy(reviews = allReviews.reviews?.filter {
                        val rating = it?.rating ?: 0
                        when(targetScore) {
                            5 -> rating > 4
                            4 -> rating > 3 && rating <= 4
                            3 -> rating > 2 && rating <= 3
                            2 -> rating > 1 && rating <= 2
                            1 -> rating <= 1
                            else -> true
                        }
                    })
                }
                Result.Success(filteredReviews!!)
            } else {
                Result.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getProductDetail(productId: Int): ProductResponse? {
        return try {
            val response = apiService.getDetailProduct(productId)
            if (response.isSuccessful) {
                response.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}