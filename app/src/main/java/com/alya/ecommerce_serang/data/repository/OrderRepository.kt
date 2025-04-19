package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.AddEvidenceRequest
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.response.cart.DataItem
import com.alya.ecommerce_serang.data.api.response.order.AddEvidenceResponse
import com.alya.ecommerce_serang.data.api.response.order.CourierCostResponse
import com.alya.ecommerce_serang.data.api.response.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.order.OrderDetailResponse
import com.alya.ecommerce_serang.data.api.response.order.OrderListResponse
import com.alya.ecommerce_serang.data.api.response.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.product.StoreProduct
import com.alya.ecommerce_serang.data.api.response.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.profile.AddressResponse
import com.alya.ecommerce_serang.data.api.response.profile.CreateAddressResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import retrofit2.Response

class OrderRepository(private val apiService: ApiService) {

    suspend fun fetchProductDetail(productId: Int): ProductResponse? {
        return try {
            val response = apiService.getDetailProduct(productId)
            if (response.isSuccessful) {
                val productResponse = response.body()
                Log.d("Order Repository", "Product detail fetched successfully: ${productResponse?.product?.productName}")
                productResponse
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error fetching product detail. Code: ${response.code()}, Error: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception fetching product", e)
            null
        }
    }

    suspend fun createOrder(orderRequest: OrderRequest): Response<CreateOrderResponse> {
        return try {
            Log.d("Order Repository", "Creating order. Request details: $orderRequest")
            val response = apiService.postOrder(orderRequest)

            if (response.isSuccessful) {
                Log.d("Order Repository", "Order created successfully. Response: ${response.body()}")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Order creation failed. Code: ${response.code()}, Error: $errorBody")
            }

            response
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception creating order", e)
            throw e
        }
    }

    suspend fun createOrderBuyNow(orderRequestBuy: OrderRequestBuy): Response<CreateOrderResponse> {
        return try {
            Log.d("Order Repository", "Creating buy now order. Request details: $orderRequestBuy")
            val response = apiService.postOrderBuyNow(orderRequestBuy)

            if (response.isSuccessful) {
                Log.d("Order Repository", "Buy now order created successfully. Response: ${response.body()}")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Buy now order creation failed. Code: ${response.code()}, Error: $errorBody")
            }

            response
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception creating buy now order", e)
            throw e
        }
    }

    suspend fun getStore(): StoreResponse? {
        return try {
            val response = apiService.getStore()

            if (response.isSuccessful) {
                val storeResponse = response.body()
                Log.d("Order Repository", "Store information fetched successfully. Store count: ${storeResponse?.store?.storeName}")
                storeResponse
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error fetching store. Code: ${response.code()}, Error: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception getting store", e)
            null
        }
    }

    suspend fun getAddress(): AddressResponse? {
        return try {
            val response = apiService.getAddress()

            if (response.isSuccessful) {
                val addressResponse = response.body()
                Log.d("Order Repository", "Address information fetched successfully. Address count: ${addressResponse?.addresses?.size}")
                addressResponse
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error fetching addresses. Code: ${response.code()}, Error: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception getting addresses", e)
            null
        }
    }

    suspend fun getCountCourierCost(courierCost: CourierCostRequest): Result<CourierCostResponse> {
        return try {
            Log.d("Order Repository", "Calculating courier cost. Request: $courierCost")
            val response = apiService.countCourierCost(courierCost)

            if (response.isSuccessful) {
                response.body()?.let { courierCostResponse ->
                    Log.d("Order Repository", "Courier cost calculation successful. Courier costs: ${courierCostResponse.courierCosts.size}")
                    Result.Success(courierCostResponse)
                } ?: run {
                    Result.Error(Exception("Failed to get courier cost: Empty response"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error calculating courier cost. Code: ${response.code()}, Error: $errorMsg")
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception calculating courier cost", e)
            Result.Error(e)
        }
    }

    suspend fun getCart(): Result<List<DataItem>> {
        return try {
            val response = apiService.getCart()

            if (response.isSuccessful) {
                val cartData = response.body()?.data
                if (!cartData.isNullOrEmpty()) {
                    Result.Success(cartData)
                } else {
                    Log.e("Order Repository", "Cart data is empty")
                    Result.Error(Exception("Cart is empty"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error fetching cart: $errorMsg")
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception fetching cart", e)
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
                    Result.Error(Exception("Store details not found"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Order Repository", "Error fetching store: $errorMsg")
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("Order Repository", "Exception fetching store details", e)
            Result.Error(e)
        }
    }

    suspend fun addAddress(request: CreateAddressRequest): Result<CreateAddressResponse> {
        return try {
            Log.d("OrderRepository", "Adding address: $request")
            val response = apiService.createAddress(request)

            if (response.isSuccessful) {
                val createAddressResponse = response.body()
                if (createAddressResponse != null) {
                    Log.d("OrderRepository", "Address added successfully: ${createAddressResponse.message}")
                    Result.Success(createAddressResponse)
                } else {
                    Log.e("OrderRepository", "Response body was null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("OrderRepository", "Error adding address: $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Exception adding address", e)
            Result.Error(e)
        }
    }

    suspend fun getListProvinces(): ListProvinceResponse? {
        val response = apiService.getListProv()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getListCities(provId : Int): ListCityResponse?{
        val response = apiService.getCityProvId(provId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun fetchUserProfile(): Result<UserProfile?> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                response.body()?.user?.let {
                    Result.Success(it)  // ✅ Returning only UserProfile
                } ?: Result.Error(Exception("User data not found"))
            } else {
                Result.Error(Exception("Error fetching profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getOrderDetails(orderId: Int): OrderDetailResponse? {
        return try {
            val response = apiService.getDetailOrder(orderId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error getting order details", e)
            null
        }
    }

    suspend fun uploadPaymentProof(request : AddEvidenceRequest): Result<AddEvidenceResponse> {
        return try {
            Log.d("OrderRepository", "Add Evidence : $request")
            val response = apiService.addEvidence(request)

            if (response.isSuccessful) {
                val addEvidenceResponse = response.body()
                if (addEvidenceResponse != null) {
                    Log.d("OrderRepository", "Add Evidence successfully: ${addEvidenceResponse.message}")
                    Result.Success(addEvidenceResponse)
                } else {
                    Log.e("OrderRepository", "Response body was null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("OrderRepository", "Error Add Evidence : $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Exception Add Evidence ", e)
            Result.Error(e)
        }
    }

    suspend fun getOrderList(status: String): Result<OrderListResponse> {
        return try {
            Log.d("OrderRepository", "Add Evidence : $status")
            val response = apiService.getOrderList(status)

            if (response.isSuccessful) {
                val allListOrder = response.body()
                if (allListOrder != null) {
                    Log.d("OrderRepository", "Add Evidence successfully: ${allListOrder.message}")
                    Result.Success(allListOrder)
                } else {
                    Log.e("OrderRepository", "Response body was null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("OrderRepository", "Error Add Evidence : $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Exception Add Evidence ", e)
            Result.Error(e)
        }
    }

}