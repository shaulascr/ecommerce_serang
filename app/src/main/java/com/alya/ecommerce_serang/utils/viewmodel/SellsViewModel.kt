package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ConfirmPaymentRequest
import com.alya.ecommerce_serang.data.api.dto.ConfirmShipmentRequest
import com.alya.ecommerce_serang.data.api.dto.OrderItemsItem
import com.alya.ecommerce_serang.data.api.response.store.sells.Orders
import com.alya.ecommerce_serang.data.api.response.store.sells.OrdersItem
import com.alya.ecommerce_serang.data.api.response.store.sells.PaymentConfirmationResponse
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.ui.order.address.ViewState
import kotlinx.coroutines.launch

class SellsViewModel(private val repository: SellsRepository) : ViewModel() {

    companion object {
        private const val TAG = "SellsViewModel"
    }

    private val _sells = MutableLiveData<ViewState<List<OrdersItem>>>()
    val sells: LiveData<ViewState<List<OrdersItem>>> = _sells

    private val _sellDetails = MutableLiveData<Orders?>()
    val sellDetails: LiveData<Orders?> get() = _sellDetails

    private val _confirmPaymentStore = MutableLiveData<Result<PaymentConfirmationResponse>>()
    val confirmPaymentStore: LiveData<Result<PaymentConfirmationResponse>> = _confirmPaymentStore

    // LiveData untuk OrderItems
    private val _orderItems = MutableLiveData<List<OrderItemsItem>?>()
    val orderItems: LiveData<List<OrderItemsItem>?> get() = _orderItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun getSellList(status: String) {
        Log.d(TAG, "========== Starting getSellList ==========")
        Log.d(TAG, "Requested status: '$status'")
        Log.d(TAG, "Repository instance: ${repository.javaClass.simpleName}")

        _sells.value = ViewState.Loading
        Log.d(TAG, "ViewState set to Loading")

        viewModelScope.launch {
            Log.d(TAG, "Coroutine launched successfully")

            try {
                Log.d(TAG, "Calling repository.getSellList(status='$status')")
                val startTime = System.currentTimeMillis()

                when (val result = repository.getSellList(status)) {
                    is Result.Success -> {
                        val endTime = System.currentTimeMillis()
                        Log.d(TAG, "Repository call completed in ${endTime - startTime}ms")
                        Log.d(TAG, "Result.Success received from repository")

                        // Log the entire result data structure
                        Log.d(TAG, "Raw result data: ${result.data}")
                        Log.d(TAG, "Result data class: ${result.data.javaClass.simpleName}")

                        val orders = result.data.orders
                        Log.d(TAG, "Extracted orders list: $orders")
                        Log.d(TAG, "Orders list class: ${orders.javaClass.simpleName}")
                        Log.d(TAG, "Orders count: ${orders.size}")

                        // Check if orders list is null or empty
                        if (false) {
                            Log.w(TAG, "⚠️ Orders list is NULL")
                        } else if (orders.isEmpty()) {
                            Log.w(TAG, "⚠️ Orders list is EMPTY")
                        } else {
                            Log.d(TAG, "✅ Orders list contains ${orders.size} items")

                            // Log individual order details with more comprehensive info
                            orders.forEachIndexed { index, order ->
                                Log.d(TAG, "--- Order ${index + 1}/${orders.size} ---")
                                Log.d(TAG, "  Order object: $order")
                                Log.d(TAG, "  Order class: ${order.javaClass.simpleName}")
                                Log.d(TAG, "  - ID: ${order.orderId}")
                                Log.d(TAG, "  - Status: '${order.status}'")
                                Log.d(TAG, "  - Customer: '${order.username}'")
                                Log.d(TAG, "  - Total: ${order.totalAmount}")
                                Log.d(TAG, "  - Items count: ${order.orderItems?.size ?: 0}")
                                Log.d(TAG, "  - Created at: ${order.createdAt}")
                                Log.d(TAG, "  - Updated at: ${order.updatedAt}")

                                // Log order items if available
                                order.orderItems?.let { items ->
                                    Log.d(TAG, "  Order items:")
                                    items.forEachIndexed { itemIndex, item ->
                                        Log.d(TAG, "    Item ${itemIndex + 1}: ${item?.productName} (Qty: ${item?.quantity})")
                                    }
                                }
                            }
                        }

                        // Set the ViewState to Success
                        _sells.value = ViewState.Success(orders)
                        Log.d(TAG, "✅ ViewState.Success set with ${orders.size} orders")
                    }

                    is Result.Error -> {
                        val endTime = System.currentTimeMillis()
                        Log.e(TAG, "Repository call failed in ${endTime - startTime}ms")
                        Log.e(TAG, "❌ Result.Error received from repository")
                        Log.e(TAG, "Error message: ${result.exception.message}")
                        Log.e(TAG, "Exception type: ${result.exception.javaClass.simpleName}")
                        Log.e(TAG, "Exception stack trace:", result.exception)

                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _sells.value = ViewState.Error(errorMessage)
                        Log.d(TAG, "ViewState.Error set with message: '$errorMessage'")
                    }

                    is Result.Loading -> {
                        Log.d(TAG, "Result.Loading received from repository (this is unusual)")
                        // Keep the current loading state
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception caught in getSellList")
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Exception stack trace:", e)

                val errorMessage = "An unexpected error occurred: ${e.message}"
                _sells.value = ViewState.Error(errorMessage)
                Log.d(TAG, "ViewState.Error set due to exception: '$errorMessage'")
            }
        }

        Log.d(TAG, "========== getSellList method completed ==========")
    }

    fun getSellDetails(orderId: Int) {
        Log.d(TAG, "========== Starting getSellDetails ==========")
        Log.d(TAG, "Fetching details for order ID: $orderId")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getSellDetails(orderId)
                if (response != null) {
                    _sellDetails.value = response.orders
                    _orderItems.value = response.orders?.orderItems?.filterNotNull()
                } else {
                    _error.value = "Gagal memuat detail pesanan"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
                Log.e(SellsViewModel.Companion.TAG, "Error fetching order details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmPayment(orderId: Int, status: String) {
        Log.d(TAG, "Confirming order completed: orderId=$orderId, status=$status")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = ConfirmPaymentRequest(orderId, status)
                val response = repository.confirmPayment(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(TAG, "confirmPayment success: ${body?.message}")
                    _message.value = body?.message ?: "Status berhasil diperbarui"
                    _isSuccess.value = true
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "confirmPayment failed: $errorMsg")
                    _error.value = "Gagal memperbarui status: $errorMsg"
                    _isSuccess.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in confirmPayment", e)
                _error.value = "Terjadi kesalahan: ${e.message}"
                _isSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmShipment(orderId: Int, receiptNum: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val request = ConfirmShipmentRequest(receiptNum, orderId)
                val response = repository.confirmShipment(request)
                if (response.isSuccessful) {
                    _message.value = response.body()?.message ?: "Berhasil mengonfirmasi pengiriman"
                    _isSuccess.value = true
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal konfirmasi pengiriman"
                    _error.value = errorMsg
                    _isSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
                _isSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshOrders(status: String = "all") {
        Log.d(TAG, "========== Starting refreshOrders ==========")
        Log.d(TAG, "Refreshing orders with status: '$status'")

        // Clear current orders before fetching new ones
        _sells.value = ViewState.Loading
        Log.d(TAG, "ViewState set to Loading for refresh")

        // Re-fetch the orders with the current status
        getSellList(status)

        Log.d(TAG, "========== refreshOrders method completed ==========")
    }
}