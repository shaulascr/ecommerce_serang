package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
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

    fun getSellList(status: String) {
        Log.d(TAG, "Requested status: '$status'")
        _sells.value = ViewState.Loading

        viewModelScope.launch {
            _sells.value = ViewState.Loading

            try {
                when (val result = repository.getSellList(status)) {
                    is Result.Success -> {
                        val orders = result.data.orders
                        Log.d(TAG, "Orders list: $orders")
                        Log.d(TAG, "Orders count: ${orders?.size ?: 0}")

                        // Log individual order details
                        orders?.forEachIndexed { index, order ->
                            Log.d(TAG, "Order $index:")
                            Log.d(TAG, "  - ID: ${order.orderId}")
                            Log.d(TAG, "  - Status: ${order.status}")
                            Log.d(TAG, "  - Customer: ${order.username}")
                            Log.d(TAG, "  - Total: ${order.totalAmount}")
                            Log.d(TAG, "  - Items count: ${order.orderItems?.size ?: 0}")
                            Log.d(TAG, "  - Updated at: ${order.updatedAt}")
                        }
                        _sells.value = ViewState.Success(result.data.orders)
                        Log.d("SellsViewModel", "Sells loaded successfully: ${result.data.orders?.size} items")
                    }
                    is Result.Error -> {
                        _sells.value = ViewState.Error(result.exception.message ?: "Unknown error occurred")
                        Log.e("SellsViewModel", "Error loading sells", result.exception)
                    }
                    is Result.Loading -> {
                        null
                    }
                }
            } catch (e: Exception) {
                _sells.value = ViewState.Error("An unexpected error occurred: ${e.message}")
                Log.e("SellsViewModel", "Exception in getOrderList", e)
            }
        }
    }

    fun updateOrderStatus(orderId: Int?, status: String) {
        Log.d(TAG, "Updating order status: orderId=$orderId, status=$status")
        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, status)
                Log.d(TAG, "Order status updated successfully: orderId=$orderId, status=$status")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order status", e)
            }
        }
    }

    fun refreshOrders(status: String = "all") {
        Log.d(TAG, "Refreshing orders with status: $status")
        // Clear current orders before fetching new ones
        _sells.value = ViewState.Loading

        // Re-fetch the orders with the current status
        getSellList(status)
    }
}