package com.alya.ecommerce_serang.ui.product

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CartItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.product.Product
import com.alya.ecommerce_serang.data.api.response.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.response.product.StoreProduct
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _productDetail = MutableLiveData<Product?>()
    val productDetail: LiveData<Product?> get() = _productDetail

    private val _storeDetail = MutableLiveData<StoreProduct?>()
    val storeDetail : LiveData<StoreProduct?> get() = _storeDetail

    private val _reviewProduct = MutableLiveData<List<ReviewsItem>>()
    val reviewProduct: LiveData<List<ReviewsItem>> get() = _reviewProduct

    private val _otherProducts = MutableLiveData<List<ProductsItem>>()
    val otherProducts: LiveData<List<ProductsItem>> get() = _otherProducts

    private val _addCart = MutableLiveData<Result<String>>()
    val addCart: LiveData<Result<String>> get() = _addCart

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    // Flag to indicate if we should navigate to checkout after adding to cart
    var shouldNavigateToCheckout: Boolean = false

    fun loadProductDetail(productId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.fetchProductDetail(productId)
                _productDetail.value = result?.product

                // Load store details if product has a store ID
//                result?.product?.storeId?.let { storeId ->
//                    loadStoreDetail(storeId)
//                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading product details: ${e.message}")
                _error.value = "Failed to load product details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

//    fun loadStoreDetail(storeId: Int) {
//        viewModelScope.launch {
//            try {
//                val result = repository.fetchStoreDetail(storeId)
//                _storeDetail.value = result
//            } catch (e: Exception) {
//                Log.e("ProductViewModel", "Error loading store details: ${e.message}")
//            }
//        }
//    }

    fun loadReviews(productId: Int) {
        viewModelScope.launch {
            try {
                val reviews = repository.fetchProductReview(productId)
                _reviewProduct.value = reviews ?: emptyList()
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading reviews: ${e.message}")
                _reviewProduct.value = emptyList()
            }
        }
    }

    fun loadOtherProducts(storeId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.getAllProducts() // Fetch products

                if (result is Result.Success) {
                    val allProducts = result.data // Extract the list
                    val filteredProducts = allProducts.filter {
                        it.storeId == storeId && it.id != _productDetail.value?.productId
                    } // Filter by storeId and exclude current product
                    _otherProducts.value = filteredProducts // Update LiveData
                } else if (result is Result.Error) {
                    Log.e("ProductViewModel", "Error loading other products: ${result.exception.message}")
                    _otherProducts.value = emptyList() // Set empty list on failure
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception loading other products: ${e.message}")
                _otherProducts.value = emptyList()
            }
        }
    }
    fun reqCart(request: CartItem){
        viewModelScope.launch {
            when (val result = repository.addToCart(request)) {
                is Result.Success -> {
                    val message = result.data.message
                    _addCart.value =
                        Result.Success(message)
                }
                is Result.Error -> {
                    _addCart.value = Result.Error(result.exception)
                }
                is Result.Loading -> {
                    // optional: already emitted above
                }
            }
        }
    }

}

//    fun loadStoreDetail(storeId: Int){
//        viewModelScope.launch {
//            val storeResult = repository.fetchStoreDetail(storeId)
//            _storeDetail.value = storeResult
//        }
//    }