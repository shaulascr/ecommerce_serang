package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.response.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.CategoryResponse
import com.alya.ecommerce_serang.data.api.response.LoginResponse
import com.alya.ecommerce_serang.data.api.response.OtpResponse
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import com.alya.ecommerce_serang.data.api.response.ProfileResponse
import com.alya.ecommerce_serang.data.api.response.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.ReviewProductResponse
import com.alya.ecommerce_serang.data.api.response.StoreResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("registeruser")
    suspend fun register (
        @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse>

    @POST("otp")
    suspend fun getOTP(
        @Body otpRequest: OtpRequest
    ):OtpResponse

    @POST("login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @GET("category")
    suspend fun allCategory(
    ): Response<CategoryResponse>

    @GET("product")
    suspend fun getAllProduct(): Response<AllProductResponse>

    @GET("product/review/{id}")
    suspend fun getProductReview(
        @Path("id") productId: Int
    ): Response<ReviewProductResponse>

    @GET("product/detail/{id}")
    suspend fun getDetailProduct (
        @Path("id") productId: Int
    ): Response<ProductResponse>

    @GET("profile")
    suspend fun getUserProfile(): Response<ProfileResponse>


    @GET("mystore")
    fun getStore (): Call<StoreResponse>
}