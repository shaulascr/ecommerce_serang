package com.alya.ecommerce_serang.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
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

    private val _productList = MutableLiveData<Result<List<ProductsItem>>>()
    val productList: LiveData<Result<List<ProductsItem>>> get() = _productList

    private val _categoryList = MutableLiveData<Result<List<CategoryItem>>>()
    val categoryList: LiveData<Result<List<CategoryItem>>> get() = _categoryList

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

    fun loadMyStoreProducts() {
        viewModelScope.launch {
            _productList.value = Result.Loading
            try {
                val result = repository.fetchMyStoreProducts()
                _productList.value = Result.Success(result)
            } catch (e: Exception) {
                _productList.value = Result.Error(e)
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categoryList.value = Result.Loading
            _categoryList.value = repository.getAllCategories()
        }
    }

    fun addProduct(
        name: String,
        description: String,
        price: Int,
        stock: Int,
        minOrder: Int,
        weight: Int,
        isPreOrder: Boolean,
        duration: Int,
        categoryId: Int,
        isActive: Boolean
    ): LiveData<Result<Unit>> = liveData {
        emit(Result.Loading)
        val result = repository.addProduct(
            name, description, price, stock, minOrder, weight, isPreOrder, duration, categoryId, isActive
        )
        emit(result)
    }



    // Optional: for store detail if you need it later
//    fun loadStoreDetail(storeId: Int) {
//        viewModelScope.launch {
//            val storeResult = repository.fetchStoreDetail(storeId)
//            _storeDetail.value = storeResult
//        }
//    }
}