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
        _sells.value = ViewState.Loading

        viewModelScope.launch {
            _sells.value = ViewState.Loading

            try {
                when (val result = repository.getSellList(status)) {
                    is Result.Success -> {
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
}