package com.alya.ecommerce_serang.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.data.api.dto.ReviewsItem
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.ReviewRepository
import com.alya.ecommerce_serang.ui.order.address.ViewState
import kotlinx.coroutines.launch
import kotlin.getOrThrow

class ReviewViewModel(private val repository: ReviewRepository) : ViewModel() {

    private val _review = MutableLiveData<ViewState<List<ReviewsItem>>>()
    val review: LiveData<ViewState<List<ReviewsItem>>> = _review

    private val _averageScore = MutableLiveData<String>()
    val averageScore: LiveData<String> = _averageScore

    private val _totalReview = MutableLiveData<Int>()
    val totalReview: LiveData<Int> = _totalReview

    private val _totalReviewWithDesc = MutableLiveData<Int>()
    val totalReviewWithDesc: LiveData<Int> = _totalReviewWithDesc

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val productImageCache = mutableMapOf<Int, String?>()

    fun getReview(score: String) {
        _review.value = ViewState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getReviewList(score)
                if (response is Result.Success) {
                    val reviews = response.data.reviews?.filterNotNull().orEmpty()
                    _review.value = ViewState.Success(reviews)

                    if (score == "all") {
                        val avg = if (reviews.isNotEmpty()) {
                            reviews.mapNotNull { it.rating }.average()
                        } else 0.0
                        _averageScore.value = String.format("%.1f", avg)
                        _totalReview.value = reviews.size
                        _totalReviewWithDesc.value = reviews.count { !it.reviewText.isNullOrBlank() }
                    }
                } else if (response is Result.Error) {
                    _review.value = ViewState.Error(response.exception.message ?: "Gagal memuat ulasan")
                }
            } catch (e: Exception) {
                _review.value = ViewState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    suspend fun getProductImage(productId: Int): String? {
        if (productImageCache.containsKey(productId)) {
            return productImageCache[productId]
        }
        val result = repository.getProductDetail(productId)
        val imageUrl = if (result?.product?.image?.startsWith("/") == true) {
            BASE_URL + result.product.image.removePrefix("/")
        } else result?.product?.image
        productImageCache[productId] = imageUrl.toString()
        return imageUrl.toString()
    }
}