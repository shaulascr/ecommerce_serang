package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.response.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.CategoryResponse
import com.alya.ecommerce_serang.data.api.response.DetailStoreProductResponse
import com.alya.ecommerce_serang.data.api.response.LoginResponse
import com.alya.ecommerce_serang.data.api.response.OtpResponse
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import com.alya.ecommerce_serang.data.api.response.ProfileResponse
import com.alya.ecommerce_serang.data.api.response.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.ReviewProductResponse
import com.alya.ecommerce_serang.data.api.response.StoreResponse
import com.alya.ecommerce_serang.data.api.response.ViewStoreProductsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @GET("store/detail/{id}")
    suspend fun getDetailStore (
        @Path("id") storeId: Int
    ): Response<DetailStoreProductResponse>


    @GET("mystore")
    suspend fun getStore (): Response<StoreResponse>

    @GET("mystore/product") // Replace with actual endpoint
    suspend fun getStoreProduct(): Response<ViewStoreProductsResponse>

    @GET("category")
    fun getCategories(): Call<CategoryResponse>

    @Multipart
    @POST("store/createproduct")
    suspend fun addProduct(
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part("min_order") minOrder: RequestBody,
        @Part("weight") weight: RequestBody,
        @Part("is_pre_order") isPreOrder: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("is_active") isActive: RequestBody,
        @Part image: MultipartBody.Part?,
        @Part sppirt: MultipartBody.Part?,
        @Part halal: MultipartBody.Part?
    ): Response<Unit>

}