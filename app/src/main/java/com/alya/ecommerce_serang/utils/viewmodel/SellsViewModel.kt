package com.alya.ecommerce_serang.utils.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.PaymentConfirmRequest
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.data.api.response.store.orders.PaymentConfirmationResponse
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

    private val _confirmPaymentStore = MutableLiveData<Result<PaymentConfirmationResponse>>()
    val confirmPaymentStore: LiveData<Result<PaymentConfirmationResponse>> = _confirmPaymentStore

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
                        Log.d(TAG, "Result data class: ${result.data?.javaClass?.simpleName}")

                        val orders = result.data.orders
                        Log.d(TAG, "Extracted orders list: $orders")
                        Log.d(TAG, "Orders list class: ${orders?.javaClass?.simpleName}")
                        Log.d(TAG, "Orders count: ${orders?.size ?: 0}")

                        // Check if orders list is null or empty
                        if (orders == null) {
                            Log.w(TAG, "⚠️ Orders list is NULL")
                        } else if (orders.isEmpty()) {
                            Log.w(TAG, "⚠️ Orders list is EMPTY")
                        } else {
                            Log.d(TAG, "✅ Orders list contains ${orders.size} items")

                            // Log individual order details with more comprehensive info
                            orders.forEachIndexed { index, order ->
                                Log.d(TAG, "--- Order ${index + 1}/${orders.size} ---")
                                Log.d(TAG, "  Order object: $order")
                                Log.d(TAG, "  Order class: ${order?.javaClass?.simpleName}")
                                Log.d(TAG, "  - ID: ${order?.orderId}")
                                Log.d(TAG, "  - Status: '${order?.status}'")
                                Log.d(TAG, "  - Customer: '${order?.username}'")
                                Log.d(TAG, "  - Total: ${order?.totalAmount}")
                                Log.d(TAG, "  - Items count: ${order?.orderItems?.size ?: 0}")
                                Log.d(TAG, "  - Created at: ${order?.createdAt}")
                                Log.d(TAG, "  - Updated at: ${order?.updatedAt}")

                                // Log order items if available
                                order?.orderItems?.let { items ->
                                    Log.d(TAG, "  Order items:")
                                    items.forEachIndexed { itemIndex, item ->
                                        Log.d(TAG, "    Item ${itemIndex + 1}: ${item?.productName} (Qty: ${item?.quantity})")
                                    }
                                }
                            }
                        }

                        // Set the ViewState to Success
                        _sells.value = ViewState.Success(orders ?: emptyList())
                        Log.d(TAG, "✅ ViewState.Success set with ${orders?.size ?: 0} orders")
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

    fun updateOrderStatus(orderId: Int?, status: String) {
        Log.d(TAG, "========== Starting updateOrderStatus ==========")
        Log.d(TAG, "Updating order status: orderId=$orderId, status='$status'")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling repository.updateOrderStatus")
                val startTime = System.currentTimeMillis()

                repository.updateOrderStatus(orderId, status)

                val endTime = System.currentTimeMillis()
                Log.d(TAG, "✅ Order status updated successfully in ${endTime - startTime}ms")
                Log.d(TAG, "Updated orderId=$orderId to status='$status'")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating order status")
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Exception stack trace:", e)
            }
        }

        Log.d(TAG, "========== updateOrderStatus method completed ==========")
    }

    fun confirmPayment(orderId: Int, status: String) {
        Log.d(TAG, "Confirming order completed: orderId=$orderId, status=$status")
        viewModelScope.launch {
            _confirmPaymentStore.value = Result.Loading
            val request = PaymentConfirmRequest(orderId, status)

            Log.d(TAG, "Sending order completion request: $request")
            val result = repository.confirmPaymentStore(request)
            Log.d(TAG, "Order completion result: $result")
            _confirmPaymentStore.value = result
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