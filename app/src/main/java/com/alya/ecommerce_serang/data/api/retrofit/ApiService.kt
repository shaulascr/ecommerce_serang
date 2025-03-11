package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.response.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.LoginResponse
import com.alya.ecommerce_serang.data.api.response.OtpResponse
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import com.alya.ecommerce_serang.data.api.response.RegisterResponse
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

    @GET("product")
    suspend fun getAllProduct(): Response<AllProductResponse>

    @GET("product/detail/{id}")
    fun getDetailProduct (
        @Path("id") productId: Int
    ): Call<ProductResponse>

    @GET("mystore")
    fun getStore (): Call<StoreResponse>
}