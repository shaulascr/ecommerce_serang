package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.UpdateCart
import com.alya.ecommerce_serang.data.api.response.ViewStoreProductsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.cart.AddCartResponse
import com.alya.ecommerce_serang.data.api.response.cart.ListCartResponse
import com.alya.ecommerce_serang.data.api.response.cart.UpdateCartResponse
import com.alya.ecommerce_serang.data.api.response.order.CourierCostResponse
import com.alya.ecommerce_serang.data.api.response.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.product.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.product.CategoryResponse
import com.alya.ecommerce_serang.data.api.response.product.DetailStoreProductResponse
import com.alya.ecommerce_serang.data.api.response.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.product.ReviewProductResponse
import com.alya.ecommerce_serang.data.api.response.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.profile.AddressResponse
import com.alya.ecommerce_serang.data.api.response.profile.CreateAddressResponse
import com.alya.ecommerce_serang.data.api.response.profile.ProfileResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
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

    @GET("cart_item")
    suspend fun getCart (): Response<ListCartResponse>

    @POST("cart/add")
    suspend fun addCart(
        @Body cartRequest: CartItem
    ): Response<AddCartResponse>

    @PUT("cart/update")
    suspend fun updateCart(
        @Body updateCart: UpdateCart
    ): Response<UpdateCartResponse>

    @POST("couriercost")
    suspend fun countCourierCost(
        @Body courierCost : CourierCostRequest
    ): Response<CourierCostResponse>

    @GET("cities/{id}")
    suspend fun getCityProvId(
        @Path("id") provId : Int
    ): Response<ListCityResponse>

    @GET("provinces")
    suspend fun getListProv(
    ): Response<ListProvinceResponse>
}