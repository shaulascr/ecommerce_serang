package com.alya.ecommerce_serang.utils.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.PaymentInfo
import com.alya.ecommerce_serang.data.repository.PaymentInfoRepository
import kotlinx.coroutines.launch
import java.io.File

class PaymentInfoViewModel(private val repository: PaymentInfoRepository) : ViewModel() {

    private val TAG = "PaymentInfoViewModel"

    private val _paymentInfos = MutableLiveData<List<PaymentInfo>>()
    val paymentInfos: LiveData<List<PaymentInfo>> = _paymentInfos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _addPaymentSuccess = MutableLiveData<Boolean>()
    val addPaymentSuccess: LiveData<Boolean> = _addPaymentSuccess

    private val _deletePaymentSuccess = MutableLiveData<Boolean>()
    val deletePaymentSuccess: LiveData<Boolean> = _deletePaymentSuccess

    var selectedBankName: String? = null
    val bankName = MutableLiveData<String>()

    fun getPaymentInfo() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading payment info...")
                val result = repository.getPaymentInfo()

                if (result.isEmpty()) {
                    Log.d(TAG, "No payment info found")
                } else {
                    Log.d(TAG, "Successfully loaded ${result.size} payment info")
                    for (method in result) {
                        Log.d(TAG, "Payment method: id=${method.id}, bank=${method.bankName}, account=${method.accountName}")
                    }
                }

                _paymentInfos.value = result
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error getting payment info", e)
                _errorMessage.value = "Gagal memuat metode pembayaran: ${e.message?.take(100) ?: "Unknown error"}"
                _isLoading.value = false
                // Still set empty payment info to show empty state
                _paymentInfos.value = emptyList()
            }
        }
    }

    fun addPaymentInfo(bankName: String, bankNumber: String, accountName: String, qrisImageUri: Uri?, qrisImageFile: File?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding payment info: bankName=$bankName, bankNumber=$bankNumber, accountName=$accountName")
                Log.d(TAG, "Image file: ${qrisImageFile?.absolutePath}, exists: ${qrisImageFile?.exists()}, size: ${qrisImageFile?.length() ?: 0} bytes")

                // Validate the file if it was provided
                if (qrisImageUri != null && qrisImageFile == null) {
                    _errorMessage.value = "Gagal memproses gambar. Silakan pilih gambar lain."
                    _isLoading.value = false
                    _addPaymentSuccess.value = false
                    return@launch
                }

                // If we have a file, make sure it exists and has some content
                if (qrisImageFile != null && (!qrisImageFile.exists() || qrisImageFile.length() == 0L)) {
                    Log.e(TAG, "Image file does not exist or is empty: ${qrisImageFile.absolutePath}")
                    _errorMessage.value = "File gambar tidak valid. Silakan pilih gambar lain."
                    _isLoading.value = false
                    _addPaymentSuccess.value = false
                    return@launch
                }

                val success = repository.addPaymentMethod(bankName, bankNumber, accountName, qrisImageFile)
                _addPaymentSuccess.value = success
                _isLoading.value = false

                if (success) {
                    // Refresh the payment info list
                    getPaymentInfo()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding payment info", e)
                _errorMessage.value = "Gagal menambahkan metode pembayaran: ${e.message?.take(100) ?: "Unknown error"}"
                _isLoading.value = false
                _addPaymentSuccess.value = false
            }
        }
    }

    fun deletePaymentInfo(paymentInfoId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = repository.deletePaymentMethod(paymentInfoId)
                _deletePaymentSuccess.value = success
                _isLoading.value = false
                if (success) {
                    // Refresh the payment info list
                    getPaymentInfo()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting payment info", e)
                _errorMessage.value = "Gagal menghapus metode pembayaran: ${e.message?.take(100) ?: "Unknown error"}"
                _isLoading.value = false
                _deletePaymentSuccess.value = false
            }
        }
    }
}