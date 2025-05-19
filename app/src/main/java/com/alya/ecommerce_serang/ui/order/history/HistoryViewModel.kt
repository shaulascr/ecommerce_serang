package com.alya.ecommerce_serang.ui.order.history

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CancelOrderReq
import com.alya.ecommerce_serang.data.api.dto.CompletedOrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrdersItem
import com.alya.ecommerce_serang.data.api.response.customer.order.CancelOrderResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderListItemsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.Orders
import com.alya.ecommerce_serang.data.api.response.order.CompletedOrderResponse
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.ui.order.address.ViewState
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel(private val repository: OrderRepository) : ViewModel()  {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _orders = MutableLiveData<ViewState<List<OrdersItem>>>()
    val orders: LiveData<ViewState<List<OrdersItem>>> = _orders

    private val _orderCompletionStatus = MutableLiveData<Result<CompletedOrderResponse>>()
    val orderCompletionStatus: LiveData<Result<CompletedOrderResponse>> = _orderCompletionStatus

    private val _orderDetails = MutableLiveData<Orders>()
    val orderDetails: LiveData<Orders> get() = _orderDetails

    private val _cancelOrderStatus = MutableLiveData<Result<CancelOrderResponse>>()
    val cancelOrderStatus: LiveData<Result<CancelOrderResponse>> = _cancelOrderStatus
    private val _isCancellingOrder = MutableLiveData<Boolean>()
    val isCancellingOrder: LiveData<Boolean> = _isCancellingOrder

    // LiveData untuk OrderItems
    private val _orderItems = MutableLiveData<List<OrderListItemsItem>>()
    val orderItems: LiveData<List<OrderListItemsItem>> get() = _orderItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun getOrderList(status: String) {
        _orders.value = ViewState.Loading
        viewModelScope.launch {
            _orders.value = ViewState.Loading

            try {
                when (val result = repository.getOrderList(status)) {
                    is Result.Success -> {
                        _orders.value = ViewState.Success(result.data.orders)
                        Log.d("HistoryViewModel", "Orders loaded successfully: ${result.data.orders.size} items")
                    }
                    is Result.Error -> {
                        _orders.value = ViewState.Error(result.exception.message ?: "Unknown error occurred")
                        Log.e("HistoryViewModel", "Error loading orders", result.exception)
                    }
                    is Result.Loading -> {
                        null
                    }
                }
            } catch (e: Exception) {
                _orders.value = ViewState.Error("An unexpected error occurred: ${e.message}")
                Log.e("HistoryViewModel", "Exception in getOrderList", e)
            }
        }
    }
    fun confirmOrderCompleted(orderId: Int, status: String) {
        Log.d(TAG, "Confirming order completed: orderId=$orderId, status=$status")
        viewModelScope.launch {
            _orderCompletionStatus.value = Result.Loading
            val request = CompletedOrderRequest(orderId, status)

            Log.d(TAG, "Sending order completion request: $request")
            val result = repository.confirmOrderCompleted(request)
            Log.d(TAG, "Order completion result: $result")
            _orderCompletionStatus.value = result
        }
    }

    fun cancelOrderWithImage(orderId: String, reason: String, imageFile: File?) {
        Log.d(TAG, "Cancelling order with image: orderId=$orderId, reason=$reason, hasImage=${imageFile != null}")
        viewModelScope.launch {
            repository.submitComplaint(orderId, reason, imageFile).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        Log.d(TAG, "Submitting complaint: Loading")
                        _isLoading.value = true
                    }
                    is Result.Success -> {
                        Log.d(TAG, "Complaint submitted successfully: ${result.data.message}")
                        _message.value = result.data.message
                        _isSuccess.value = true
                        _isLoading.value = false
                    }
                    is Result.Error -> {
                        val errorMessage = result.exception.message ?: "Error submitting complaint"
                        Log.e(TAG, "Error submitting complaint: $errorMessage", result.exception)
                        _message.value = errorMessage
                        _isSuccess.value = false
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun getOrderDetails(orderId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getOrderDetails(orderId)
                if (response != null) {
                    _orderDetails.value = response.orders
                    _orderItems.value = response.orders.orderItems
                } else {
                    _error.value = "Gagal memuat detail pesanan"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
                Log.e(TAG, "Error fetching order details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelOrder(cancelReq: CancelOrderReq) {
        viewModelScope.launch {
            try {
                _cancelOrderStatus.value = Result.Loading
                val result = repository.cancelOrder(cancelReq)
                _cancelOrderStatus.value = result
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error cancelling order: ${e.message}")
                _cancelOrderStatus.value = Result.Error(e)
            }
        }
    }

    fun refreshOrders(status: String = "all") {
        Log.d(TAG, "Refreshing orders with status: $status")
        // Clear current orders before fetching new ones
        _orders.value = ViewState.Loading

        // Re-fetch the orders with the current status
        getOrderList(status)
    }
}