package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.dto.OrdersItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.cart.DataItem
import com.alya.ecommerce_serang.data.api.response.customer.order.CourierCostResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.CreateOrderResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.ProductResponse
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreProduct
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.CreateAddressResponse
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

    suspend fun addAddress(createAddressRequest: CreateAddressRequest): Result<CreateAddressResponse> {
        return try {
            val response = apiService.createAddress(createAddressRequest)
            if (response.isSuccessful){
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Add Address failed"))
            } else {
                Log.e("OrderRepository", "Error: ${response.errorBody()?.string()}")
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
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

    suspend fun fetchSells(): List<OrdersItem?> {
        return try {
            val response = apiService.getAllOrders() // Replace with the actual method from your ApiService
            if (response.isSuccessful) {
                response.body()?.orders ?: emptyList() // Assuming the response body has 'orders'
            } else {
                Log.e("OrderRepository", "Error fetching all sells. Code: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Exception fetching sells", e)
            emptyList()
        }
    }

    suspend fun fetchOrdersByStatus(status: String): List<OrdersItem?> {
        return try {
            val response = apiService.getOrdersByStatus(status) // Replace with actual method for status-based fetch
            if (response.isSuccessful) {
                response.body()?.orders?.filterNotNull() ?: emptyList() // Assuming the response body has 'orders'
            } else {
                Log.e("OrderRepository", "Error fetching orders by status ($status). Code: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Exception fetching orders by status", e)
            emptyList()
        }
    }
}