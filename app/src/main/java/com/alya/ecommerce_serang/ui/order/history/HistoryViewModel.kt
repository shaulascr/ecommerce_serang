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
import com.alya.ecommerce_serang.data.api.response.customer.order.OrderListResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.Orders
import com.alya.ecommerce_serang.data.api.response.order.CompletedOrderResponse
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.ui.order.address.ViewState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryViewModel(private val repository: OrderRepository) : ViewModel()  {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

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

    private val _selectedStatus = MutableStateFlow("all")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    val orders: StateFlow<ViewState<List<OrdersItem>>> =
        _selectedStatus
            .flatMapLatest { status ->
                flow<ViewState<List<OrdersItem>>> {
                    Log.d(TAG, "⏳  Loading orders for status = $status")
                    emit(ViewState.Loading)

                    val viewState =
                        if (status == "all") {
                            getAllOrdersCombined().also {
                                Log.d(TAG, "✅  Combined orders size = ${(it as? ViewState.Success)?.data?.size}")
                            }
                        } else {
                            when (val r = repository.getOrderList(status)) {

                                is Result.Loading -> {
                                    Log.d(TAG, "   repository.getOrderList($status) → Loading")
                                    ViewState.Loading
                                }

                                is Result.Success -> {
                                    Log.d(TAG, "✅  repository.getOrderList($status) success, size = ${r.data.orders.size}")
                                    // Tag each order so the fragment’s filter works
                                    val tagged = r.data.orders.onEach { it.displayStatus = status }
                                    ViewState.Success(tagged)
                                }

                                is Result.Error -> {
                                    Log.e(TAG, "❌  repository.getOrderList($status) error = ${r.exception.message}")
                                    ViewState.Error(r.exception.message ?: "Unknown error")
                                }
                            }
                        }

                    emit(viewState)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ViewState.Loading                          // ② initial value, still fine
            )

    private suspend fun getAllOrdersCombined(): ViewState<List<OrdersItem>> = try {
        val statuses = listOf("unpaid", "paid", "processed", "shipped", "completed", "canceled")

        val all = coroutineScope {
            statuses
                .map { status ->
                    async {
                        when (val r = repository.getOrderList(status)) {
                            is Result.Success -> r.data.orders.onEach { it.displayStatus = status }
                            else              -> emptyList()
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }

        val sorted = all.sortedByDescending { order ->
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(order.createdAt)
            } catch (_: Exception) { null }
        }

        ViewState.Success(sorted)
    } catch (e: Exception) {
        ViewState.Error("Failed to load orders: ${e.message}")
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


    fun updateStatus(status: String, forceRefresh: Boolean = false) {
        Log.d(TAG, "↪️  updateStatus(status = $status, forceRefresh = $forceRefresh)")

        // No‑op guard (optional): skip if user re‑selects same tab and no refresh asked
        if (_selectedStatus.value == status && !forceRefresh) {
            Log.d(TAG, "🔸  Status unchanged & forceRefresh = false → skip update")
            return
        }

        _selectedStatus.value = status
        Log.d(TAG, "✅  _selectedStatus set to \"$status\"")

        if (forceRefresh) {
            Log.d(TAG, "🔄  forceRefresh = true → launching refresh()")
            viewModelScope.launch { refresh(status) }
        }
    }

    fun refresh(status: String) {
        Log.d(TAG, "⏳  refresh(\"$status\") started")

        try {
            viewModelScope.launch {
                if (status == "all") {
                    Log.d(TAG, "🌐  Calling getAllOrdersCombined()")
                    getAllOrdersCombined()                      // network → cache
                } else {
                    Log.d(TAG, "🌐  repository.getOrderList(\"$status\")")
                    repository.getOrderList(status)            // network → cache
                }
                Log.d(TAG, "✅  refresh(\"$status\") completed (repository updated)")
                // Flow that watches DB/cache will emit automatically
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌  refresh(\"$status\") failed: ${e.message}", e)
        }
    }

    private fun Result<OrderListResponse>.toViewState(): ViewState<List<OrdersItem>> =
        when (this) {
            is Result.Success -> ViewState.Success(data.orders)
            is Result.Error   -> ViewState.Error(exception.message ?: "Unknown error")
            is Result.Loading -> ViewState.Loading     // should rarely reach UI
        }
}