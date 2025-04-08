package com.alya.ecommerce_serang.ui.order

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.response.AddressesItem
import com.alya.ecommerce_serang.data.api.response.OrderResponse
import com.alya.ecommerce_serang.data.api.response.PaymentItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class CheckoutViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _checkoutData = MutableLiveData<CheckoutData>()
    val checkoutData: LiveData<CheckoutData> = _checkoutData

    private val _addressDetails = MutableLiveData<AddressesItem>()
    val addressDetails: LiveData<AddressesItem> = _addressDetails

    private val _storePayments = MutableLiveData<List<PaymentItem>>()
    val storePayments: LiveData<List<PaymentItem>> = _storePayments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCheckoutData(orderRequest: OrderRequest) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Load all necessary data
                val productDetails = repository.fetchProductDetail(orderRequest.productIdItem)
                val storeDetails = repository.getStoreDetails(productDetails.product.storeId)

//                val addressDetails = repository.getAddressDetails(orderRequest.address_id)

                // Update LiveData objects
//                _addressDetails.value = addressDetails

                // Create CheckoutData object
                _checkoutData.value = CheckoutData(
                    orderRequest = orderRequest,
                    productName = productDetails?.product?.productName,
                    productImageUrl = productDetails.product.image,
                    productPrice = productDetails.product.price,
                    sellerName = storeDetails.store.storeName,
                    sellerImageUrl = storeDetails.store.storeImage,
                    sellerId = productDetails.product.storeId
                )

                storeDetails?.let {
                    _storePayments.value = it.payment
                }

            } catch (e: Exception) {
                // Handle errors
                Log.e("CheckoutViewModel", "Error loading checkout data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun calculateTotal(): Double {
        val data = checkoutData.value ?: return 0.0
        return (data.productPrice * data.orderRequest.quantity) + data.orderRequest.shipPrice
    }
}