package com.alya.ecommerce_serang.ui.product

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.product.Product
import com.alya.ecommerce_serang.data.api.response.product.ReviewsItem
import com.alya.ecommerce_serang.data.api.response.product.Store
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _productDetail = MutableLiveData<Product?>()
    val productDetail: LiveData<Product?> get() = _productDetail

    private val _storeDetail = MutableLiveData<Store?>()
    val storeDetail : LiveData<Store?> get() = _storeDetail

    private val _reviewProduct = MutableLiveData<List<ReviewsItem>>()
    val reviewProduct: LiveData<List<ReviewsItem>> get() = _reviewProduct

    private val _otherProducts = MutableLiveData<List<ProductsItem>>()
    val otherProducts: LiveData<List<ProductsItem>> get() = _otherProducts

    fun loadProductDetail(productId: Int) {
        viewModelScope.launch {
            val result = repository.fetchProductDetail(productId)
            _productDetail.value = result?.product
        }
    }

    fun loadReviews(productId: Int) {
        viewModelScope.launch {
            val reviews = repository.fetchProductReview(productId)
            _reviewProduct.value = reviews ?: emptyList()
        }
    }

    fun loadOtherProducts(storeId: Int) {
        viewModelScope.launch {
            val result = repository.getAllProducts() // Fetch products

            if (result is Result.Success) {
                val allProducts = result.data // Extract the list
                val filteredProducts = allProducts.filter { it.storeId == storeId } // Filter by storeId
                _otherProducts.value = filteredProducts // Update LiveData
            } else if (result is Result.Error) {
                Log.e("ProductViewModel", "Error loading other products: ${result.exception.message}")
                _otherProducts.value = emptyList() // Set empty list on failure
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