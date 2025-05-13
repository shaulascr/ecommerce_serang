package com.alya.ecommerce_serang.ui.order.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.ReviewProductItem
import com.alya.ecommerce_serang.data.api.response.customer.order.CreateReviewResponse
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class CreateReviewViewModel(private val repository: OrderRepository): ViewModel() {
    private val _reviewSubmitStatus = MutableLiveData<Result<CreateReviewResponse>>()
    val reviewSubmitStatus: LiveData<Result<CreateReviewResponse>> = _reviewSubmitStatus

    private val _reviewsSubmitted = MutableLiveData(0)
    private var totalReviewsToSubmit = 0
    private var anyFailures = false

    fun submitReview(reviewItem: ReviewProductItem) {
        viewModelScope.launch {
            try {
                _reviewSubmitStatus.value = Result.Loading
                val result = repository.createReviewProduct(reviewItem)
                _reviewSubmitStatus.value = result
            } catch (e: Exception) {
                anyFailures = true
                Log.e("CreateReviewViewModel", "Error create review: ${e.message}")
                _reviewSubmitStatus.value = Result.Error(e)
            }
        }
    }

    fun setTotalReviewsToSubmit(count: Int) {
        totalReviewsToSubmit = count
        _reviewsSubmitted.value = 0
        anyFailures = false
    }
}