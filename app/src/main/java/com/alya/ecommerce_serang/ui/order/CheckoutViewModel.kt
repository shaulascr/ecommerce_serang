package com.alya.ecommerce_serang.ui.order

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
import com.alya.ecommerce_serang.data.api.response.customer.cart.DataItemCart
import com.alya.ecommerce_serang.data.api.response.customer.product.PaymentItemDetail
import com.alya.ecommerce_serang.data.api.response.customer.profile.AddressesItem
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class CheckoutViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _checkoutData = MutableLiveData<CheckoutData>()
    val checkoutData: LiveData<CheckoutData> = _checkoutData

    private val _addressDetails = MutableLiveData<AddressesItem?>()
    val addressDetails: LiveData<AddressesItem?> = _addressDetails

    private val _availablePaymentMethods = MutableLiveData<List<PaymentItemDetail>>()
    val availablePaymentMethods: LiveData<List<PaymentItemDetail>> = _availablePaymentMethods

    // Selected payment method
    private val _selectedPayment = MutableLiveData<PaymentItemDetail?>()
    val selectedPayment: LiveData<PaymentItemDetail?> = _selectedPayment

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _orderCreated = MutableLiveData<Boolean>()
    val orderCreated: LiveData<Boolean> = _orderCreated



    // Initialize "Buy Now" checkout
    fun initializeBuyNow(
        storeId: Int,
        storeName: String?,
        productId: Int,
        productName: String?,
        productImage: String?,
        quantity: Int,
        price: Double,
        isWholesale: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Create initial OrderRequestBuy object
                val orderRequest = OrderRequestBuy(
                    addressId = 0, // Will be set when user selects address
                    paymentMethodId = 0, // Will be set when user selects payment
                    shipPrice = 0, // Will be set when user selects shipping
                    shipName = "",
                    shipService = "",
                    isNego = false, // Default value
                    productId = productId,
                    quantity = quantity,
                    shipEtd = "",
                    isReseller = isWholesale
                )

                // Create checkout data
                _checkoutData.value = CheckoutData(
                    orderRequest = orderRequest,
                    productName = productName,
                    productImageUrl = productImage ?: "",
                    productPrice = price,
                    sellerName = storeName ?: "",
                    sellerId = storeId,
                    quantity = quantity,
                    isBuyNow = true
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Buy Now data", e)
                _errorMessage.value = "Failed to initialize checkout: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Initialize checkout from cart
    fun initializeFromCart(cartItemIds: List<Int>, isWholesaleMap: Map<Int, Boolean> = emptyMap()) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Get cart data
                val cartResult = repository.getCart()

                if (cartResult is Result.Success) {
                    // Find matching cart items
                    val matchingItems = mutableListOf<CartItemsItem>()
                    var storeData: DataItemCart? = null

                    for (store in cartResult.data) {
                        val storeItems = store.cartItems.filter { it.cartItemId in cartItemIds }
                        if (storeItems.isNotEmpty()) {
                            matchingItems.addAll(storeItems)
                            storeData = store
                            break
                        }
                    }

                    if (matchingItems.isNotEmpty() && storeData != null) {
                        // Create initial OrderRequest object
                        val orderRequest = OrderRequest(
                            addressId = 0,
                            paymentMethodId = 0,
                            shipPrice = 0,
                            shipName = "",
                            shipService = "",
                            isNego = false,
                            cartItemId = cartItemIds,
                            shipEtd = "",
                            // Add a list tracking which items are wholesale
                            isReseller = isWholesaleMap.any { it.value } // Set true if any item is wholesale
                        )

                        // Create checkout data
                        _checkoutData.value = CheckoutData(
                            orderRequest = orderRequest,
                            productName = matchingItems.first().productName,
                            sellerName = storeData.storeName,
                            sellerId = storeData.storeId,
                            isBuyNow = false,
                            cartItems = matchingItems,
                            cartItemWholesaleMap = isWholesaleMap // Store the wholesale map
                        )

                        calculateSubtotal()
                        calculateTotal()
                    } else {
                        _errorMessage.value = "No matching cart items found"
                    }
                } else if (cartResult is Result.Error) {
                    _errorMessage.value = "Failed to fetch cart items: ${cartResult.exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getPaymentMethods() {
        viewModelScope.launch {
            try {
                val storeId = _checkoutData.value?.sellerId ?: run {
                    Log.e(TAG, "StoreId is null - cannot fetch payment methods")
                    _availablePaymentMethods.value = emptyList()
                    return@launch
                }

                Log.d(TAG, "Attempting to fetch payment methods for storeId: $storeId")

                if (storeId <= 0) {
                    Log.e(TAG, "Invalid storeId: $storeId - cannot fetch payment methods")
                    _availablePaymentMethods.value = emptyList()
                    return@launch
                }

                val result = repository.fetchPaymentStore(storeId)

                when (result) {
                    is Result.Success -> {
                        val paymentMethods = result.data?.filterNotNull() ?: emptyList()

                        Log.d(TAG, "Fetched ${paymentMethods.size} payment methods")

                        // Update payment methods
                        _availablePaymentMethods.value = paymentMethods
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error fetching payment methods: ${result.exception.message}")
                        _availablePaymentMethods.value = emptyList()
                    }
                    is Result.Loading -> {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getPaymentMethods", e)
                _availablePaymentMethods.value = emptyList()
            }
        }
    }

    // Updated setPaymentMethod function
    fun setPaymentMethod(paymentId: Int) {
        viewModelScope.launch {
            try {
                // Get the available payment methods
                val paymentMethods = _availablePaymentMethods.value

                if (paymentMethods.isNullOrEmpty()) {
                    // If no payment methods available, try to fetch them
                    getPaymentMethods()
                    return@launch
                }

                val selectedPayment = paymentMethods.find { it.id == paymentId }

                if (selectedPayment == null) {
                    Log.e(TAG, "Payment with ID $paymentId not found")
                    return@launch
                }

                // Set the selected payment - IMPORTANT: do this first
                _selectedPayment.value = selectedPayment
                Log.d(TAG, "Payment selected: ID=${selectedPayment.id}, Name=${selectedPayment.bankName}")

                // Update the order request with the payment method ID
                val currentData = _checkoutData.value ?: return@launch

                // Different handling for Buy Now vs Cart checkout
                if (currentData.isBuyNow) {
                    // For Buy Now checkout
                    val buyRequest = currentData.orderRequest as OrderRequestBuy
                    val updatedRequest = buyRequest.copy(paymentMethodId = paymentId)
                    _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
                } else {
                    // For Cart checkout
                    val cartRequest = currentData.orderRequest as OrderRequest
                    val updatedRequest = cartRequest.copy(paymentMethodId = paymentId)
                    _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error setting payment method: ${e.message}"
                Log.e(TAG, "Error setting payment method", e)
            }
        }
    }

    // Set selected address
    fun setSelectedAddress(addressId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get address details from API
                val addressResponse = repository.getAddress()
                if (addressResponse != null && !addressResponse.addresses.isNullOrEmpty()) {
                    val address = addressResponse.addresses.find { it.id == addressId }
                    if (addressResponse != null && !addressResponse.addresses.isNullOrEmpty()) {
                        val address = addressResponse.addresses.find { it.id == addressId }
                        // No need for null check since _addressDetails now accepts nullable values
                        _addressDetails.value = address

                        // Update order request with address ID only if address isn't null
                        if (address != null) {
                            val currentData = _checkoutData.value ?: return@launch
                            if (currentData.isBuyNow) {
                                val buyRequest = currentData.orderRequest as OrderRequestBuy
                                val updatedRequest = buyRequest.copy(addressId = addressId)
                                _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
                            } else {
                                val cartRequest = currentData.orderRequest as OrderRequest
                                val updatedRequest = cartRequest.copy(addressId = addressId)
                                _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading address: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Set shipping method
    fun setShippingMethod(shipName: String, shipService: String, shipPrice: Int, shipEtd: String) {
        val currentData = _checkoutData.value ?: return

        if (currentData.isBuyNow) {
            val buyRequest = currentData.orderRequest as OrderRequestBuy
            val updatedRequest = buyRequest.copy(
                shipName = shipName,
                shipService = shipService,
                shipPrice = shipPrice,
                shipEtd = shipEtd
            )
            _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
        } else {
            val cartRequest = currentData.orderRequest as OrderRequest
            val updatedRequest = cartRequest.copy(
                shipName = shipName,
                shipService = shipService,
                shipPrice = shipPrice,
                shipEtd = shipEtd
            )
            _checkoutData.value = currentData.copy(orderRequest = updatedRequest)
        }
    }

    // Create order
    fun createOrder() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val data = _checkoutData.value ?: throw Exception("No checkout data available")

                if (data.orderRequest is OrderRequest) {
                    val request = data.orderRequest

                    // Check for required fields
                    if (request.addressId <= 0) {
                        _errorMessage.value = "Please select a delivery address"
                        _isLoading.value = false
                        return@launch
                    }

                    if (request.paymentMethodId <= 0) {
                        _errorMessage.value = "Please select a payment method"
                        _isLoading.value = false
                        return@launch
                    }

                    if (request.shipPrice <= 0 || request.shipName.isBlank() || request.shipService.isBlank()) {
                        _errorMessage.value = "Please select a shipping method"
                        _isLoading.value = false
                        return@launch
                    }
                } else if (data.orderRequest is OrderRequestBuy) {
                    val request = data.orderRequest

                    // Similar validation for buy now
                    if (request.addressId <= 0 || request.paymentMethodId <= 0 ||
                        request.shipPrice <= 0 || request.shipName.isBlank() || request.shipService.isBlank()) {
                        _errorMessage.value = "Please complete all required checkout information"
                        _isLoading.value = false
                        return@launch
                    }
                }

                val response = if (data.isBuyNow) {
                    // For Buy Now, use the dedicated endpoint
                    val buyRequest = data.orderRequest as OrderRequestBuy
                    repository.createOrderBuyNow(buyRequest)
                } else {
                    // For Cart checkout, use the standard order endpoint
                    val cartRequest = data.orderRequest as OrderRequest
                    repository.createOrder(cartRequest)
                }

                if (response.isSuccessful) {
                    _orderCreated.value = true
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _errorMessage.value = "Failed to create order: $errorMsg"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Calculate total price (subtotal + shipping)
    fun calculateTotal(): Double {
        val data = _checkoutData.value ?: return 0.0

        return calculateSubtotal() + getShippingPrice()
    }

    // Calculate subtotal (without shipping)
    fun calculateSubtotal(): Double {
        val data = _checkoutData.value ?: return 0.0

        return if (data.isBuyNow) {
            // For Buy Now, use product price * quantity
            val buyRequest = data.orderRequest as OrderRequestBuy
            data.productPrice * buyRequest.quantity
        } else {
            // For Cart, sum all items
            data.cartItems.sumOf { it.price * it.quantity.toDouble() }
        }
    }

    // Get shipping price
    private fun getShippingPrice(): Double {
        val data = _checkoutData.value ?: return 0.0

        return if (data.isBuyNow) {
            (data.orderRequest as OrderRequestBuy).shipPrice.toDouble()
        } else {
            (data.orderRequest as OrderRequest).shipPrice.toDouble()
        }
    }



    companion object {
        private const val TAG = "CheckoutViewModel"
    }
}