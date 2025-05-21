package com.alya.ecommerce_serang.ui.order.history.detailorder

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CompletedOrderRequest
import com.alya.ecommerce_serang.data.api.response.customer.order.Orders
import com.alya.ecommerce_serang.data.repository.OrderRepository
import kotlinx.coroutines.launch
import java.io.File

class DetailOrderViewModel(private val orderRepository: OrderRepository): ViewModel() {

    private val _orderDetails = MutableLiveData<Orders>()
    val orderDetails: LiveData<Orders> = _orderDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun getOrderDetails(orderId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = orderRepository.getOrderDetails(orderId)
                if (response != null) {
                    _orderDetails.value = response.orders
                } else {
                    _error.value = "Failed to load order details"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e("DetailOrderViewModel", "Error loading order details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmOrderCompleted(detailOrderRequest: CompletedOrderRequest) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                orderRepository.confirmOrderCompleted(detailOrderRequest)
                _isSuccess.value = true
                _message.value = "Order status updated successfully"

                getOrderDetails(detailOrderRequest.orderId)

            } catch (e: Exception) {
                _isSuccess.value = false
                _message.value = "Error: ${e.message}"
                Log.e("DetailOrderViewModel", "Error updating order status", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelOrderWithImage(orderId: Int, reason: String, imageFile: File?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                orderRepository.submitComplaint(orderId.toString(), reason, imageFile)
                _isSuccess.value = true
                _message.value = "Order canceled successfully"

            } catch (e: Exception) {
                _isSuccess.value = false
                _message.value = "Error: ${e.message}"
                Log.e("DetailOrderViewModel", "Error canceling order", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}