package com.alya.ecommerce_serang.data.repository

import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.PaymentUpdate
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.dto.ShippingServiceRequest
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.store.StoreResponse
import com.alya.ecommerce_serang.data.api.response.store.profile.StoreDataResponse
import com.alya.ecommerce_serang.data.api.response.store.sells.OrderListResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

class MyStoreRepository(private val apiService: ApiService) {
    suspend fun fetchMyStoreProfile(): Result<StoreResponse?> {
        return try {
            val response = apiService.getMyStoreData()

            if (response.isSuccessful) {
                val storeResponse = response.body()
                Result.Success(storeResponse)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown API error"
                Log.e("MyStoreRepository", "Error: $errorMessage")
                Result.Error(HttpException(response))
            }
        } catch (e: IOException) {
            Log.e("MyStoreRepository", "Network error: ${e.message}")
            Result.Error(e)
        } catch (e: Exception) {
            Log.e("MyStoreRepository", "Unexpected error: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun listStoreType(): Result<ListStoreTypeResponse>{
        return try{
            val response = apiService.listTypeStore()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("No store type"))
            } else {
                throw Exception("No response ${response.errorBody()?.string()}")
            }
        } catch (e:Exception){
            Result.Error(e)
        }
    }

    suspend fun updateStoreProfile(
        storeName: RequestBody,
        storeDescription: RequestBody,
        isOnLeave: RequestBody,
        storeType: RequestBody,
        storeimg: MultipartBody.Part?
    ): Response<StoreDataResponse>? {

        return try {
            Log.d(TAG, "storeName: $storeName")
            Log.d(TAG, "storeDescription: $storeDescription")
            Log.d(TAG, "isOnLeave: $isOnLeave")
            Log.d(TAG, "storeType: $storeType")
            Log.d(TAG, "storeimg: ${storeimg?.headers}")

            apiService.updateStoreProfileMultipart(
                storeName, storeDescription, isOnLeave, storeType, storeimg
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating store profile", e)
            null
        }
    }

    suspend fun getSellList(status: String): Result<OrderListResponse> {
        return try {
            Log.d(TAG, "Add Evidence : $status")
            val response = apiService.getSellList(status)

            if (response.isSuccessful) {
                val allListSell = response.body()
                if (allListSell != null) {
                    Log.d(TAG, "Add Evidence successfully: ${allListSell.message}")
                    Result.Success(allListSell)
                } else {
                    Log.e(TAG, "Response body was null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error Add Evidence : $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception Add Evidence ", e)
            Result.Error(e)
        }
    }

    suspend fun getBalance(): Result<com.alya.ecommerce_serang.data.api.response.store.StoreResponse> {
        return try {
            val response = apiService.getMyStoreData()

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.Error(IllegalStateException("Response body is null"))

                // Validate the balance field
                val balanceRaw = body.store.balance
                balanceRaw.toDoubleOrNull()
                    ?: return Result.Error(NumberFormatException("Invalid balance format: $balanceRaw"))

                Result.Success(body)
            } else {
                Result.Error(
                    Exception("Failed to load balance: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching balance", e)
            Result.Error(e)
        }
    }

    suspend fun fetchMyStoreProducts(): List<ProductsItem> {
        return try {
            val response = apiService.getStoreProduct()
            if (response.isSuccessful) {
                response.body()?.products?.filterNotNull() ?: emptyList()
            } else {
                throw Exception("Failed to fetch store products: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching store products", e)
            throw e
        }
    }

    suspend fun updateStoreApproval(
        storeName: RequestBody,
        description: RequestBody,
        storeType: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        storeProvince: RequestBody,
        storeCity: RequestBody,
        storeSubdistrict: RequestBody,
        storeVillage: RequestBody,
        storeStreet: RequestBody,
        storePostalCode: RequestBody,
        storeAddressDetail: RequestBody,
        userPhone: RequestBody,
        paymentsToUpdate: List<PaymentUpdate> = emptyList(),
        paymentIdToDelete: List<Int> = emptyList(),
        storeCourier: List<String>? = null,
        storeImage: MultipartBody.Part?,
        ktpImage: MultipartBody.Part?,
        npwpDocument: MultipartBody.Part?,
        nibDocument: MultipartBody.Part?
    ): Response<StoreDataResponse>? {
        return try {
            Log.d(TAG, "Updating store profile & address for approval...")

            val profileResp = apiService.updateStoreApprovalMultipart(
                storeName = storeName,
                storeDescription = description,
                storeTypeId = storeType,
                storeLatitude = latitude,
                storeLongitude = longitude,
                storeProvince = storeProvince,
                storeCity = storeCity,
                storeSubdistrict = storeSubdistrict,
                storeVillage = storeVillage,
                storeStreet = storeStreet,
                storePostalCode = storePostalCode,
                storeAddressDetail = storeAddressDetail,
                storeUserPhone = userPhone,
                storeimg = storeImage,
                ktp = ktpImage,
                npwp = npwpDocument,
                nib = nibDocument
            )

            if (!profileResp.isSuccessful) {
                Log.e(TAG, "Profile update failed: ${profileResp.code()} ${profileResp.errorBody()?.string()}")
                return profileResp // short-circuit; let caller inspect the failure
            }

            // 2) Payments: delete, then upsert (safer if you’re changing accounts)
            if (paymentIdToDelete.isNotEmpty() || paymentsToUpdate.isNotEmpty()) {
                Log.d(TAG, "Synchronizing payments: delete=${paymentIdToDelete.size}, upsert=${paymentsToUpdate.size}")
            }

            // 2a) Delete payments
            paymentIdToDelete.forEach { id ->
                runCatching {
                    apiService.deletePaymentInfo(id)
                }.onSuccess {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Delete payment $id failed: ${it.code()} ${it.errorBody()?.string()}")
                    } else {
                        Log.d(TAG, "Deleted payment $id")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Delete payment $id exception", e)
                }
            }

            // 2b) Upsert payments (add if id==null, else update)
            paymentsToUpdate.forEach { item ->
                runCatching {
                    // --- CHANGE HERE if your PaymentUpdate field names differ ---
                    val id = item.id            // Int?  (null => add)
                    val bankName = item.bankName // String
                    val bankNum = item.bankNum   // String
                    val accountName = item.accountName // String
                    val qrisImage = item.qrisImage // File? (Optional)
                    // -----------------------------------------------------------

                    if (id == null) {
                        // ADD
                        val resp = apiService.addPaymentInfoDirect(
                            bankName = bankName.toPlain(),
                            bankNum = bankNum.toPlain(),
                            accountName = accountName.toPlain(),
                            qris = createQrisPartOrNull(qrisImage)
                        )
                        if (!resp.isSuccessful) {
                            Log.e(TAG, "Add payment failed: ${resp.code()} ${resp.errorBody()?.string()}")
                        } else {
                            Log.d(TAG, "Added payment: $bankName/$bankNum")
                        }
                    } else {
                        // UPDATE
                        val resp = apiService.updatePaymentInfo(
                            paymentInfoId = id.toString().toPlain(),
                            accountName = accountName.toPlain(),
                            bankName = bankName.toPlain(),
                            bankNum = bankNum.toPlain(),
                            qris = createQrisPartOrNull(qrisImage)
                        )
                        if (!resp.isSuccessful) {
                            Log.e(TAG, "Update payment $id failed: ${resp.code()} ${resp.errorBody()?.string()}")
                        } else {
                            Log.d(TAG, "Updated payment $id: $bankName/$bankNum")
                        }
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Upsert payment exception", e)
                }
            }

            // 3) Shipping: sync to desiredCouriers (if provided)
            storeCourier?.let { desired ->
                try {
                    val current = apiService.getStoreData().let { resp ->
                        if (resp.isSuccessful) {
                            resp.body()?.shipping?.mapNotNull { it.courier } ?: emptyList()
                        } else {
                            Log.e(TAG, "Failed to read current shipping: ${resp.code()} ${resp.errorBody()?.string()}")
                            emptyList()
                        }
                    }

                    val desiredSet = desired.toSet()
                    val currentSet = current.toSet()

                    val toAdd = (desiredSet - currentSet).toList()
                    val toDel = (currentSet - desiredSet).toList()

                    if (toAdd.isNotEmpty()) {
                        val addResp = apiService.addShippingService(ShippingServiceRequest(couriers = toAdd))
                        if (!addResp.isSuccessful) {
                            Log.e(TAG, "Add couriers failed: ${addResp.code()} ${addResp.errorBody()?.string()}")
                        } else {
                            Log.d(TAG, "Added couriers: $toAdd")
                        }
                    }

                    if (toDel.isNotEmpty()) {
                        val delResp = apiService.deleteShippingService(ShippingServiceRequest(couriers = toDel))
                        if (!delResp.isSuccessful) {
                            Log.e(TAG, "Delete couriers failed: ${delResp.code()} ${delResp.errorBody()?.string()}")
                        } else {
                            Log.d(TAG, "Deleted couriers: $toDel")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sync shipping exception", e)
                }
            }

            // Return the profile response (already successful here)
            profileResp
        } catch (e: Exception) {
            Log.e(TAG, "Error updating store approval flow", e)
            null
        }
    }

    private fun String.toPlain(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun createQrisPartOrNull(file: File?): MultipartBody.Part? =
        file?.let {
            val mime = when (it.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }.toMediaTypeOrNull()

            MultipartBody.Part.createFormData(
                "qris",
                it.name,
                it.asRequestBody(mime)
            )
        }

    companion object {
        private var TAG = "MyStoreRepository"
    }

//    private fun fetchBalance() {
//        showLoading(true)
//        lifecycleScope.launch {
//            try {
//                val response = ApiConfig.getApiService(sessionManager).getMyStoreData()
//                if (response.isSuccessful && response.body() != null) {
//                    val storeData = response.body()!!
//                    val balance = storeData.store.balance
//
//                    // Format the balance
//                    try {
//                        val balanceValue = balance.toDouble()
//                        binding.tvBalance.text = String.format("Rp%,.0f", balanceValue)
//                    } catch (e: Exception) {
//                        binding.tvBalance.text = "Rp$balance"
//                    }
//                } else {
//                    Toast.makeText(
//                        this@BalanceActivity,
//                        "Gagal memuat data saldo: ${response.message()}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error fetching balance", e)
//                Toast.makeText(
//                    this@BalanceActivity,
//                    "Error: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
}