package com.alya.ecommerce_serang.ui.order.history

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.response.order.OrdersItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.ui.order.address.ViewState
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: OrderRepository) : ViewModel()  {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _orders = MutableLiveData<ViewState<List<OrdersItem>>>()
    val orders: LiveData<ViewState<List<OrdersItem>>> = _orders

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
}