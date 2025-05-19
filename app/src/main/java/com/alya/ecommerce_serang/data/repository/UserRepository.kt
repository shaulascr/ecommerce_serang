package com.alya.ecommerce_serang.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.response.auth.HasStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.VerifRegisterResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.EditProfileResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.utils.FileUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UserRepository(private val apiService: ApiService) {
    //post data without message/response
    suspend fun requestOtpRep(email: String): OtpResponse {
        return apiService.getOTP(OtpRequest(email))
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

    suspend fun getListProvinces(): ListProvinceResponse? {
        val response = apiService.getListProv()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getListCities(provId : Int): ListCityResponse? {
        val response = apiService.getCityProvId(provId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun registerUser(request: RegisterRequest): String {
        val response = apiService.register(request) // API call

        if (response.isSuccessful) {
            val responseBody = response.body() ?: throw Exception("Empty response body")
            return responseBody.message // Get the message from RegisterResponse
        } else {
            throw Exception("Registration failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun registerStoreUser(
        context: Context,
        description: String,
        storeTypeId: Int,
        latitude: String,
        longitude: String,
        street: String,
        subdistrict: String,
        cityId: Int,
        provinceId: Int,
        postalCode: Int,
        detail: String?,
        bankName: String,
        bankNum: Int,
        storeName: String,
        storeImg: Uri?,
        ktp: Uri?,
        npwp: Uri?,
        nib: Uri?,
        persetujuan: Uri?,
        couriers: List<String>,
        qris: Uri?,
        accountName: String
    ): Result<RegisterStoreResponse> {
        return try {
            val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val storeTypeIdPart = storeTypeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudePart = latitude.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = longitude.toRequestBody("text/plain".toMediaTypeOrNull())
            val streetPart = street.toRequestBody("text/plain".toMediaTypeOrNull())
            val subdistrictPart = subdistrict.toRequestBody("text/plain".toMediaTypeOrNull())
            val cityIdPart = cityId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val provinceIdPart = provinceId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val postalCodePart = postalCode.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val detailPart = detail?.toRequestBody("text/plain".toMediaTypeOrNull())
            val bankNamePart = bankName.toRequestBody("text/plain".toMediaTypeOrNull())
            val bankNumPart = bankNum.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val storeNamePart = storeName.toRequestBody("text/plain".toMediaTypeOrNull())
            val accountNamePart = accountName.toRequestBody("text/plain".toMediaTypeOrNull())


            // Create a Map for courier values
            val courierMap = HashMap<String, RequestBody>()
            couriers.forEach { courier ->
                courierMap["couriers[]"] = courier.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            // Convert URIs to MultipartBody.Part
            val storeImgPart = storeImg?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "store_img_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("storeimg", file.name, requestFile)
            }

            val ktpPart = ktp?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "ktp_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("ktp", file.name, requestFile)
            }

            val npwpPart = npwp?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "npwp_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("npwp", file.name, requestFile)
            }

            val nibPart = nib?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "nib_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("nib", file.name, requestFile)
            }

            val persetujuanPart = persetujuan?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "persetujuan_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("persetujuan", file.name, requestFile)
            }

            val qrisPart = qris?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "qris_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("qris", file.name, requestFile)
            }

            // Make the API call
            val response = apiService.registerStore(
                descriptionPart,
                storeTypeIdPart,
                latitudePart,
                longitudePart,
                streetPart,
                subdistrictPart,
                cityIdPart,
                provinceIdPart,
                postalCodePart,
                detailPart ?: "".toRequestBody("text/plain".toMediaTypeOrNull()),
                bankNamePart,
                bankNumPart,
                storeNamePart,
                storeImgPart,
                ktpPart,
                npwpPart,
                nibPart,
                persetujuanPart,
                courierMap,
                qrisPart,
                accountNamePart
            )

            // Check if response is successful
            if (response.isSuccessful) {
                Result.Success(response.body() ?: throw Exception("Response body is null"))
            } else {
                Result.Error(Exception("Registration failed with code: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Login response is empty"))
            } else {
                Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchUserProfile(): Result<UserProfile?> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                response.body()?.user?.let {
                    Result.Success(it)  // ✅ Returning only UserProfile
                } ?: Result.Error(Exception("User data not found"))
            } else {
                Result.Error(Exception("Error fetching profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun editProfileCust(
        context: Context,
        username: String,
        name: String,
        phone: String,
        birthDate: String,
        email: String,
        imageUri: Uri?
    ): Result<EditProfileResponse> {
        return try {
            // Log the data being sent
            Log.d(TAG, "Edit Profile - Username: $username, Name: $name, Phone: $phone, Birth Date: $birthDate, Email: $email")
            Log.d(TAG, "Image URI: $imageUri")

            // Create RequestBody objects for text fields
            val usernameRequestBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneRequestBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val birthDateRequestBody = birthDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailRequestBody = email.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create MultipartBody.Part for the image
            val imagePart = if (imageUri != null) {
                // Create a temporary file from the URI using the utility class
                val imageFile = FileUtils.createTempFileFromUri(context, imageUri, "profile")
                if (imageFile != null) {
                    // Create MultipartBody.Part from the file
                    FileUtils.createMultipartFromFile("userimg", imageFile)
                } else {
                    // Fallback to empty part
                    FileUtils.createEmptyMultipart("userimg")
                }
            } else {
                // No image selected, use empty part
                FileUtils.createEmptyMultipart("userimg")
            }

            // Make the API call
            val response = apiService.editProfileCustomer(
                username = usernameRequestBody,
                name = nameRequestBody,
                phone = phoneRequestBody,
                birthDate = birthDateRequestBody,
                userimg = imagePart,
                email = emailRequestBody
            )

            // Process the response
            if (response.isSuccessful) {
                val editResponse = response.body()
                if (editResponse != null) {
                    Log.d(TAG, "Edit profile success: ${editResponse.message}")
                    Result.Success(editResponse)
                } else {
                    Log.e(TAG, "Response body is null")
                    Result.Error(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown Error"
                Log.e(TAG, "Error editing profile: $errorBody")
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in editProfileCust: ${e.message}")
            e.printStackTrace()
            Result.Error(e)
        }
    }
    
    suspend fun checkStore(): HasStoreResponse{
        return apiService.checkStoreUser()
    }

    suspend fun checkValue(request: VerifRegisReq): VerifRegisterResponse{
        return apiService.verifValue(request)
    }

    companion object{
        private const val TAG = "UserRepository"
    }


}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}