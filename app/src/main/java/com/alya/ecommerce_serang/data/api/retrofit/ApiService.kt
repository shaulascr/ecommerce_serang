package com.alya.ecommerce_serang.data.api.retrofit


import com.alya.ecommerce_serang.data.api.dto.AddEvidenceRequest
import com.alya.ecommerce_serang.data.api.dto.AddPaymentInfoResponse
import com.alya.ecommerce_serang.data.api.dto.CancelOrderReq
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.CityResponse
import com.alya.ecommerce_serang.data.api.dto.CompletedOrderRequest
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.FcmReq
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.PaymentConfirmRequest
import com.alya.ecommerce_serang.data.api.dto.ProvinceResponse
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.ReviewProductItem
import com.alya.ecommerce_serang.data.api.dto.SearchRequest
import com.alya.ecommerce_serang.data.api.dto.ShippingServiceRequest
import com.alya.ecommerce_serang.data.api.dto.StoreAddressResponse
import com.alya.ecommerce_serang.data.api.dto.UpdateCart
import com.alya.ecommerce_serang.data.api.dto.UpdateChatRequest
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.response.auth.CheckStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.FcmTokenResponse
import com.alya.ecommerce_serang.data.api.response.auth.HasStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.ListNotifResponse
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreNotifResponse
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.VerifRegisterResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatHistoryResponse
import com.alya.ecommerce_serang.data.api.response.chat.ChatListResponse
import com.alya.ecommerce_serang.data.api.response.chat.SendChatResponse
import com.alya.ecommerce_serang.data.api.response.chat.UpdateChatResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.AddCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.DeleteCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.ListCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.cart.UpdateCartResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CancelOrderResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CourierCostResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CreateReviewResponse
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
import com.alya.ecommerce_serang.data.api.response.customer.profile.EditProfileResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.ProfileResponse
import com.alya.ecommerce_serang.data.api.response.order.AddEvidenceResponse
import com.alya.ecommerce_serang.data.api.response.order.ComplaintResponse
import com.alya.ecommerce_serang.data.api.response.order.CompletedOrderResponse
import com.alya.ecommerce_serang.data.api.response.product.CreateSearchResponse
import com.alya.ecommerce_serang.data.api.response.product.SearchHistoryResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.PaymentConfirmationResponse
import com.alya.ecommerce_serang.data.api.response.store.product.CreateProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.DeleteProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.UpdateProductResponse
import com.alya.ecommerce_serang.data.api.response.store.product.ViewStoreProductsResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.GenericResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
import com.alya.ecommerce_serang.data.api.response.store.topup.BalanceTopUpResponse
import com.alya.ecommerce_serang.data.api.response.store.topup.TopUpResponse
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
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("registeruser")
    suspend fun register (
        @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse>

    @POST("verif")
    suspend fun verifValue (
        @Body verifRegisReq: VerifRegisReq
    ):VerifRegisterResponse

    @GET("checkstore")
    suspend fun checkStore (): Response<CheckStoreResponse>

    @Multipart
    @POST("registerstore")
    suspend fun registerStore(
        @Part("description") description: RequestBody,
        @Part("store_type_id") storeTypeId: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("street") street: RequestBody,
        @Part("subdistrict") subdistrict: RequestBody,
        @Part("city_id") cityId: RequestBody,
        @Part("province_id") provinceId: RequestBody,
        @Part("postal_code") postalCode: RequestBody,
        @Part("detail") detail: RequestBody,
        @Part("bank_name") bankName: RequestBody,
        @Part("bank_num") bankNum: RequestBody,
        @Part("store_name") storeName: RequestBody,
        @Part storeimg: MultipartBody.Part?,
        @Part ktp: MultipartBody.Part?,
        @Part npwp: MultipartBody.Part?,
        @Part nib: MultipartBody.Part?,
        @Part persetujuan: MultipartBody.Part?,
        @PartMap couriers: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part qris: MultipartBody.Part?,
        @Part("account_name") accountName: RequestBody,
    ): Response<RegisterStoreResponse>

    @POST("otp")
    suspend fun getOTP(
        @Body otpRequest: OtpRequest
    ):OtpResponse

    @PUT("updatefcm")
    suspend fun updateFcm(
        @Body fcmReq: FcmReq
    ): FcmTokenResponse

    @GET("checkstore")
    suspend fun checkStoreUser(
    ): HasStoreResponse

    @POST("login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @GET("category")
    suspend fun allCategory(
    ): Response<CategoryResponse>

    @GET("storetype")
    suspend fun listTypeStore(
    ): Response<ListStoreTypeResponse>

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

    @POST("order/cancel")
    suspend fun cancelOrder(
        @Body cancelReq: CancelOrderReq
    ): Response<CancelOrderResponse>

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

    @Multipart
    @PUT("profile/edit")
    suspend fun editProfileCustomer(
        @Part("username") username: RequestBody,
        @Part("name") name: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("birth_date") birthDate: RequestBody,
        @Part userimg: MultipartBody.Part,
        @Part("email") email: RequestBody
    ): Response<EditProfileResponse>

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
    suspend fun getStore(): Response<StoreResponse>

    @GET("mystore")
    suspend fun getStoreData(): Response<StoreDataResponse>

    @GET("mystore")
    suspend fun getMyStoreData(): Response<com.alya.ecommerce_serang.data.api.response.store.StoreResponse>

    @GET("mystore")
    suspend fun getStoreAddress(): Response<StoreAddressResponse>

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
        @Part("is_wholesale") isWholesale: RequestBody,
        @Part("wholesale_min_item") minItemWholesale: RequestBody,
        @Part("wholesale_price") wholesalePrice: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("status") status: RequestBody,
        @Part("condition") condition: RequestBody,
        @Part image: MultipartBody.Part?,
        @Part sppirt: MultipartBody.Part?,
        @Part halal: MultipartBody.Part?
    ): Response<CreateProductResponse>

    @Multipart
    @PUT("store/editproduct")
    suspend fun updateProduct(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part productImage: MultipartBody.Part?,
        @Part halal: MultipartBody.Part?,
        @Part sppirt: MultipartBody.Part?
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

    @DELETE("cart/delete/{id}")
    suspend fun deleteCart(
        @Path("id") cartItemId : Int
    ):Response<DeleteCartResponse>

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

    @GET("mystore/orders/{status}")
    suspend fun getSellList(
        @Path("status") status: String
    ): Response<com.alya.ecommerce_serang.data.api.response.store.sells.OrderListResponse>

    @GET("order/detail/{id}")
    suspend fun getSellDetail(
        @Path("id") orderId: Int
    ): Response<com.alya.ecommerce_serang.data.api.response.store.sells.OrderDetailResponse>

    @PUT("store/order/update")
    suspend fun confirmOrder(
        @Body confirmOrder : CompletedOrderRequest
    ): Response<CompletedOrderResponse>

    @PUT("store/order/update")
    suspend fun updateOrder(
        @Query("order_id") orderId: Int?,
        @Query("status") status: String
    ): Response<com.alya.ecommerce_serang.data.api.response.store.sells.UpdateOrderItemResponse>

    @Multipart
    @POST("addcomplaint")
    suspend fun addComplaint(
        @Part("order_id") orderId: RequestBody,
        @Part("description") description: RequestBody,
        @Part complaintimg: MultipartBody.Part
    ): Response<ComplaintResponse>

    @POST("review")
    suspend fun createReview(
        @Body contentReview : ReviewProductItem
    ): Response<CreateReviewResponse>

    @GET("store/topup")
    suspend fun getTopUpHistory(): Response<TopUpResponse>

    @GET("store/topup")
    suspend fun getFilteredTopUpHistory(@Query("date") date: String): Response<TopUpResponse>

    @Multipart
    @POST("store/createtopup")
    suspend fun addBalanceTopUp(
        @Part topupimg: MultipartBody.Part,
        @Part("amount") amount: RequestBody,
        @Part("payment_info_id") paymentInfoId: RequestBody,
        @Part("transaction_date") transactionDate: RequestBody,
        @Part("bank_name") bankName: RequestBody,
        @Part("bank_num") bankNum: RequestBody
    ): Response<BalanceTopUpResponse>

    @PUT("store/payment/update")
    suspend fun paymentConfirmation(
        @Body confirmPaymentReq : PaymentConfirmRequest
    ): Response<PaymentConfirmationResponse>

    @Multipart
    @PUT("mystore/edit")
    suspend fun updateStoreProfileMultipart(
        @Part("store_name") storeName: RequestBody,
        @Part("store_status") storeStatus: RequestBody,
        @Part("store_description") storeDescription: RequestBody,
        @Part("is_on_leave") isOnLeave: RequestBody,
        @Part("city_id") cityId: RequestBody,
        @Part("province_id") provinceId: RequestBody,
        @Part("street") street: RequestBody,
        @Part("subdistrict") subdistrict: RequestBody,
        @Part("detail") detail: RequestBody,
        @Part("postal_code") postalCode: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("user_phone") userPhone: RequestBody,
        @Part("store_type_id") storeTypeId: RequestBody,
        @Part storeimg: MultipartBody.Part?
    ): Response<StoreDataResponse>

    @Multipart
    @POST("mystore/payment/add")
    suspend fun addPaymentInfo(
        @Part("bank_name") bankName: RequestBody,
        @Part("bank_num") bankNum: RequestBody,
        @Part("account_name") accountName: RequestBody,
        @Part qris: MultipartBody.Part?
    ): Response<GenericResponse>

    @Multipart
    @POST("mystore/payment/add")
    suspend fun addPaymentInfoDirect(
        @Part("bank_name") bankName: RequestBody,
        @Part("bank_num") bankNum: RequestBody,
        @Part("account_name") accountName: RequestBody,
        @Part qris: MultipartBody.Part?
    ): Response<AddPaymentInfoResponse>

    @DELETE("mystore/payment/delete/{id}")
    suspend fun deletePaymentInfo(
        @Path("id") paymentMethodId: Int
    ): Response<GenericResponse>

    // Shipping Service API endpoints
    @POST("mystore/shipping/add")
    suspend fun addShippingService(
        @Body request: ShippingServiceRequest
    ): Response<GenericResponse>

    @POST("mystore/shipping/delete")
    suspend fun deleteShippingService(
        @Body request: ShippingServiceRequest
    ): Response<GenericResponse>

    @GET("provinces")
    suspend fun getProvinces(): Response<ProvinceResponse>

    @GET("cities/{provinceId}")
    suspend fun getCities(
        @Path("provinceId") provinceId: String
    ): Response<CityResponse>

    @PUT("mystore/edit")
    suspend fun updateStoreAddress(
        @Body addressData: HashMap<String, Any?>
    ): Response<StoreAddressResponse>

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

    @Multipart
    @POST("store/sendchat")
    suspend fun sendChatMessageStore(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part chatimg: MultipartBody.Part? = null
    ): Response<SendChatResponse>

    @GET("store/chat")
    suspend fun getChatListStore(
    ): Response<ChatListResponse>

    @Multipart
    @POST("sendchat")
    suspend fun sendChatMessage(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part chatimg: MultipartBody.Part? = null
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

    @GET("notification")
    suspend fun getNotif(
    ): Response<ListNotifResponse>

    @GET("mystore/notification")
    suspend fun getNotifStore(
    ): Response<ListStoreNotifResponse>
}