package com.alya.ecommerce_serang.ui.order

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CostProduct
import com.alya.ecommerce_serang.data.api.dto.CourierCostRequest
import com.alya.ecommerce_serang.data.api.response.customer.order.CourierCostsItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShippingViewModel(
    private val repository: OrderRepository
) : ViewModel() {

    // Shipping options LiveData
    private val _shippingOptions = MutableLiveData<List<CourierCostsItem>>()
    val shippingOptions: LiveData<List<CourierCostsItem>> = _shippingOptions

    // Loading state LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message LiveData
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    /**
     * Load shipping options based on address, product, and quantity
     */
//    fun loadShippingOptions(addressId: Int, productId: Int, quantity: Int) {
//        _isLoading.value = true
//        _errorMessage.value = ""
//
//        val costProduct = CostProduct(
//            productId = productId,
//            quantity = quantity
//        )
//
//        viewModelScope.launch {
//            // Define the courier services to try
//            val courierServices = listOf("pos", "jne", "tiki")
//
//            // Create a mutable list to collect successful courier options
//            val availableCourierOptions = mutableListOf<CourierCostsItem>()
//
//            // Try each courier service
//            for (courier in courierServices) {
//                try {
//                    // Create a request for this specific courier
//                    val courierRequest = CourierCostRequest(
//                        addressId = addressId,
//                        itemCost = listOf(costProduct),
//                        courier = courier  // Add the courier to the request
//                    )
//
//                    // Make a separate API call for each courier
//                    val result = repository.getCountCourierCost(courierRequest)
//
//                    when (result) {
//                        is Result.Success -> {
//                            // Add this courier's options to our collection
//                            result.data.courierCosts?.let { costs ->
//                                availableCourierOptions.addAll(costs)
//                            }
//                            // Update UI with what we have so far
//                            _shippingOptions.value = availableCourierOptions
//                        }
//                        is Result.Error -> {
//                            // Log the error but continue with next courier
//                            Log.e("ShippingViewModel", "Error fetching cost for courier $courier: ${result.exception.message}")
//                        }
//                        is Result.Loading -> {
//                            // Handle loading state
//                        }
//                    }
//                } catch (e: Exception) {
//                    // Log the exception but continue with next courier
//                    Log.e("ShippingViewModel", "Exception for courier $courier: ${e.message}")
//                }
//            }
//
//            // Show error only if we couldn't get any shipping options
//            if (availableCourierOptions.isEmpty()) {
//                _errorMessage.value = "No shipping options available. Please try again later."
//            }
//
//            _isLoading.value = false
//        }
//    }

    fun loadShippingOptions(addressId: Int, productId: Int, quantity: Int) {
        _isLoading.value = true
        _errorMessage.value = ""

        val costProduct = CostProduct(
            productId = productId,
            quantity = quantity
        )

        val request = CourierCostRequest(
            addressId = addressId,
            itemCost = listOf(costProduct)
        )

        viewModelScope.launch {
            var success = false
            var attempt = 0
            val maxAttempts = 3

            while (!success && attempt < maxAttempts) {
                attempt++

                try {
                    val result = repository.getCountCourierCost(request)

                    when (result) {
                        is Result.Success -> {
                            _shippingOptions.value = result.data.courierCosts
                            success = true
                        }
                        is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                            Log.e("ShippingViewModel", "Attempt $attempt failed: ${result.exception.message}")
                            // Wait before retrying
                            delay(120000)
                        }
                        is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                            // Handle loading state
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ShippingViewModel", "Attempt $attempt exception: ${e.message}")
                    // Wait before retrying
                    delay(1000)
                }
            }

            // After all attempts, check if we have any shipping options
            if (!success || _shippingOptions.value.isNullOrEmpty()) {
                _errorMessage.value = "No shipping options available. Please try again later."
            }

            _isLoading.value = false
        }
    }
}