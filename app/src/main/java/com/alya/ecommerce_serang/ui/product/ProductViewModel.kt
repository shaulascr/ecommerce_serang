package com.alya.ecommerce_serang.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.Product
import com.alya.ecommerce_serang.data.api.response.ReviewsItem
import com.alya.ecommerce_serang.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _productDetail = MutableLiveData<Product?>()
    val productDetail: LiveData<Product?> get() = _productDetail

    private val _storeDetail = MutableLiveData<Store?>()
    val storeDetail : LiveData<Store?> get() = _storeDetail

    private val _reviewProduct = MutableLiveData<List<ReviewsItem>>()
    val reviewProduct: LiveData<List<ReviewsItem>> get() = _reviewProduct

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
}

//    fun loadStoreDetail(storeId: Int){
//        viewModelScope.launch {
//            val storeResult = repository.fetchStoreDetail(storeId)
//            _storeDetail.value = storeResult
//        }
//    }