package com.alya.ecommerce_serang.data.api.retrofit


import com.alya.ecommerce_serang.data.api.dto.AddEvidenceRequest
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.CompletedOrderRequest
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.SearchRequest
import com.alya.ecommerce_serang.data.api.dto.UpdateCart
import com.alya.ecommerce_serang.data.api.dto.UpdateChatRequest
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatHistoryResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatListResponse
import com.alya.ecommerce_serang.data.api.response.chat.SendChatResponse
import com.alya.ecommerce_serang.data.api.response.chat.UpdateChatResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.AddCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.ListCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.UpdateCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CourierCostResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderDetailResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderListResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.CategoryResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.DetailStoreProductResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.ReviewProductResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.CreateAddressResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.ProfileResponse
import com.alya.ecommerce_serang.data.api.response.order.AddEvidenceResponse
import com.alya.ecommerce_serang.data.api.response.order.ComplaintResponse
import com.alya.ecommerce_serang.data.api.response.order.CompletedOrderResponse
import com.alya.ecommerce_serang.data.api.response.product.CreateSearchResponse
import com.alya.ecommerce_serang.data.api.response.product.SearchHistoryResponse
import com.alya.ecommerce_serang.data.api.response.store.product.CreateProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.DeleteProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.UpdateProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.ViewStoreProductsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("order")
    suspend fun postOrder(
        @Body request: OrderRequest
    ): Response<CreateOrderResponse>

    @GET("order/detail/{id}")
    suspend fun getDetailOrder(
        @Path("id") orderId: Int
    ): Response<OrderDetailResponse>

    @POST("order/addevidence")
    suspend fun addEvidence(
        @Body request : AddEvidenceRequest,
    ): Response<AddEvidenceResponse>

    @Multipart
    @POST("order/addevidence")
    suspend fun addEvidenceMultipart(
        @Part("order_id") orderId: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part evidence: MultipartBody.Part
    ): Response<AddEvidenceResponse>

    @GET("order/{status}")
    suspend fun getOrderList(
        @Path("status") status: String
    ):Response<OrderListResponse>

    @POST("order")
    suspend fun postOrderBuyNow(
        @Body request: OrderRequestBuy
    ): Response<CreateOrderResponse>

    @GET("profile/address")
    suspend fun getAddress(
    ): Response<AddressResponse>

    @POST("profile/addaddress")
    suspend fun createAddress(
        @Body createAddressRequest: CreateAddressRequest
    ): Response<CreateAddressResponse>

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
        @Part("status") status: RequestBody,
        @Part("condition") condition: RequestBody,
        @Part image: MultipartBody.Part?,
        @Part sppirt: MultipartBody.Part?,
        @Part halal: MultipartBody.Part?
    ): Response<CreateProductResponse>

    @PUT("store/editproduct/{id}")
    suspend fun updateProduct(
        @Path("id") productId: Int?,
        @Body updatedProduct: Map<String, Any?>
    ): Response<UpdateProductResponse>

    @DELETE("store/deleteproduct/{id}")
    suspend fun deleteProduct(
        @Path("id") productId: Int
    ): Response<DeleteProductResponse>

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

    @GET("order/{status}")
    suspend fun getSellList(
        @Path("status") status: String
    ): Response<com.alya.ecommerce_serang.data.api.response.store.orders.OrderListResponse>

    @PUT("store/order/update")
    suspend fun confirmOrder(
        @Body confirmOrder : CompletedOrderRequest
    ): Response<CompletedOrderResponse>

    @PUT("store/order/update")
    suspend fun updateOrder(
        @Query("order_id") orderId: Int?,
        @Query("status") status: String
    ): Response<com.alya.ecommerce_serang.data.api.response.store.orders.UpdateOrderItemResponse>

    @Multipart
    @POST("addcomplaint")
    suspend fun addComplaint(
        @Part("order_id") orderId: RequestBody,
        @Part("description") description: RequestBody,
        @Part complaintimg: MultipartBody.Part
    ): Response<ComplaintResponse>

    @POST("search")
    suspend fun saveSearchQuery(
        @Body searchRequest: SearchRequest
    ): Response<CreateSearchResponse>

    @GET("search")
    suspend fun getSearchHistory(): Response<SearchHistoryResponse>

    @Multipart
    @POST("sendchat")
    suspend fun sendChatLine(
        @Part("store_id") storeId: RequestBody,
        @Part("message") message: RequestBody,
        @Part("product_id") productId: RequestBody?,
        @Part chatimg: MultipartBody.Part?
    ): Response<SendChatResponse>

    @PUT("chatstatus")
    suspend fun updateChatStatus(
        @Body request: UpdateChatRequest
    ): Response<UpdateChatResponse>

    @GET("chat/{chatRoomId}")
    suspend fun getChatDetail(
        @Path("chatRoomId") chatRoomId: Int
    ): Response<ChatHistoryResponse>

    @GET("chat")
    suspend fun getChatList(
    ): Response<ChatListResponse>
}