package com.alya.ecommerce_serang.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.response.order.ServicesItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShippingViewModel(private val repository: OrderRepository): ViewModel() {

    private val _shippingServices = MutableStateFlow<Result<List<ServicesItem>>?>(null)
    val shippingServices: StateFlow<Result<List<ServicesItem>>?> = _shippingServices

    fun fetchShippingServices(request: CourierCostRequest) {
        viewModelScope.launch {
            val result = repository.getCountCourierCost(request)
            if (result is Result.Success) {
                val services = result.data.courierCosts.flatMap { courier ->
                    courier.services.map {
                        it.copy(service = "${courier.courier} - ${it.service}")
                    }
                }
                _shippingServices.value = Result.Success(services)
            } else if (result is Result.Error) {
                _shippingServices.value = Result.Error(result.exception)
            }
        }
    }
}