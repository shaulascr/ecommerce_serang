package com.alya.ecommerce_serang.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.alya.ecommerce_serang.data.api.dto.FcmReq
import com.alya.ecommerce_serang.data.api.dto.LoginRequest
import com.alya.ecommerce_serang.data.api.dto.OtpRequest
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.response.auth.FcmTokenResponse
import com.alya.ecommerce_serang.data.api.response.auth.HasStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.ListStoreTypeResponse
import com.alya.ecommerce_serang.data.api.response.auth.LoginResponse
import com.alya.ecommerce_serang.data.api.response.auth.NotifItem
import com.alya.ecommerce_serang.data.api.response.auth.NotifstoreItem
import com.alya.ecommerce_serang.data.api.response.auth.OtpResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterResponse
import com.alya.ecommerce_serang.data.api.response.auth.RegisterStoreResponse
import com.alya.ecommerce_serang.data.api.response.auth.VerifRegisterResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListCityResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.ListProvinceResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.SubdistrictResponse
import com.alya.ecommerce_serang.data.api.response.customer.order.VillagesResponse
import com.alya.ecommerce_serang.data.api.response.customer.profile.EditProfileResponse
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.utils.FileUtils
import com.alya.ecommerce_serang.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UserRepository(private val apiService: ApiService) {

    private val ALLOWED_FILE_TYPES = Regex("^(jpeg|jpg|png|pdf)$", RegexOption.IGNORE_CASE)

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

    suspend fun getListSubdistrict(cityId : String): SubdistrictResponse? {
        val response = apiService.getSubdistrict(cityId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getListVillages(subId: String): VillagesResponse? {
        val response = apiService.getVillages(subId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun registerUser(request: RegisterRequest): RegisterResponse {
        val response = apiService.register(request) // API call

        if (response.isSuccessful) {
            val responseBody = response.body() ?: throw Exception("Empty response body")
            return responseBody // Get the message from RegisterResponse
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
        cityId: String,
        provinceId: Int,
        postalCode: Int,
        detail: String,
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
            Log.d("RegisterStoreRepo", "Registration params: " +
                    "storeName=$storeName, " +
                    "storeTypeId=$storeTypeId, " +
                    "location=($latitude,$longitude), " +
                    "address=$street, $subdistrict, cityId=$cityId, provinceId=$provinceId, " +
                    "postalCode=$postalCode, " +
                    "bankDetails=$bankName, $bankNum, $accountName, " +
                    "couriers=${couriers.joinToString()}, " +
                    "files: storeImg=${storeImg != null}, ktp=${ktp != null}, npwp=${npwp != null}, " +
                    "nib=${nib != null}, persetujuan=${persetujuan != null}, qris=${qris != null}")

            val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val storeTypeIdPart = storeTypeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudePart = latitude.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = longitude.toRequestBody("text/plain".toMediaTypeOrNull())
            val streetPart = street.toRequestBody("text/plain".toMediaTypeOrNull())
            val subdistrictPart = subdistrict.toRequestBody("text/plain".toMediaTypeOrNull())
            val cityIdPart = cityId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val provinceIdPart = provinceId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val postalCodePart = postalCode.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val detailPart = detail.toRequestBody("text/plain".toMediaTypeOrNull())
            val bankNamePart = bankName.toRequestBody("text/plain".toMediaTypeOrNull())
            val bankNumPart = bankNum.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val storeNamePart = storeName.toRequestBody("text/plain".toMediaTypeOrNull())
            val accountNamePart = accountName.toRequestBody("text/plain".toMediaTypeOrNull())


            // Create a Map for courier values
            val courierMap = HashMap<String, RequestBody>()
            couriers.forEachIndexed { index, courier ->
                // Add index to make keys unique
                courierMap["couriers[$index]"] = courier.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            val storeImgPart = try {
                processImageFile(context, storeImg, "storeimg", "store_img")
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("Foto toko: ${e.message}"))
            }

            val ktpPart = try {
                processImageFile(context, ktp, "ktp", "ktp")
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("KTP: ${e.message}"))
            }

            val npwpPart = try {
                npwp?.let {
                    processDocumentFile(context, it, "npwp", "npwp")
                }
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("NPWP: ${e.message}"))
            }

            val nibPart = try {
                processDocumentFile(context, nib, "nib", "nib")
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("NIB: ${e.message}"))
            }

            val persetujuanPart = try {
                persetujuan?.let {
                    processDocumentFile(context, it, "persetujuan", "persetujuan")
                }
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("Persetujuan: ${e.message}"))
            }

            val qrisPart = try {
                qris?.let {
                    processDocumentFile(context, it, "qris", "qris")
                }
            } catch (e: IllegalArgumentException) {
                return Result.Error(Exception("QRIS: ${e.message}"))
            }

            Log.d("RegisterStoreRepo", "All parts prepared, making API call")


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
                detailPart,
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
                Log.d("RegisterStoreRepo", "Registration successful")
                Result.Success(response.body() ?: throw Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error details"
                Log.e("RegisterStore", "Registration failed: ${response.code()}, Error: $errorBody")
                Result.Error(Exception("Registration failed with code: ${response.code()}\nDetails: $errorBody"))
            }

        } catch (e: Exception) {
            Log.e("RegisterStoreRepo", "Registration exception", e)
            Result.Error(e)
        }
    }

    private suspend fun processImageFile(
        context: Context,
        uri: Uri?,
        formName: String,
        filePrefix: String
    ): MultipartBody.Part? {
        if (uri == null) {
            Log.d(TAG, "$formName is null, skipping")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing $formName image")

                // Check file type
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                Log.d(TAG, "$formName MIME type: $mimeType")

                // Validate file type
                if (!ImageUtils.isAllowedFileType(context, uri, ALLOWED_FILE_TYPES)) {
                    Log.e(TAG, "$formName has invalid file type: $mimeType")
                    throw IllegalArgumentException("$formName hanya menerima file JPEG, JPG, atau PNG")
                }

                // Only compress image files, not PDFs
                if (mimeType.startsWith("image/")) {
                    Log.d(TAG, "Compressing $formName image")

                    // Compress image
                    val compressedFile = ImageUtils.compressImage(
                        context = context,
                        uri = uri,
                        filename = filePrefix,
                        maxWidth = 1024,
                        maxHeight = 1024,
                        quality = 80
                    )

                    val requestFile = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    Log.d(TAG, "$formName compressed size: ${compressedFile.length() / 1024} KB")

                    MultipartBody.Part.createFormData(formName, compressedFile.name, requestFile)
                } else {
                    throw IllegalArgumentException("$formName harus berupa file gambar (JPEG, JPG, atau PNG)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing $formName image", e)
                throw e
            }
        }
    }

    // Process document files (handle PDFs separately)
    private suspend fun processDocumentFile(
        context: Context,
        uri: Uri?,
        formName: String,
        filePrefix: String
    ): MultipartBody.Part? {
        if (uri == null) {
            Log.d(TAG, "$formName is null, skipping")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing $formName document")

                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                Log.d(TAG, "$formName MIME type: $mimeType")

                // Validate file type
                if (!ImageUtils.isAllowedFileType(context, uri, ALLOWED_FILE_TYPES)) {
                    Log.e(TAG, "$formName has invalid file type: $mimeType")
                    throw IllegalArgumentException("$formName hanya menerima file JPEG, JPG, PNG, atau PDF")
                }

                // For image documents, compress them
                if (mimeType.startsWith("image/")) {
                    return@withContext processImageFile(context, uri, formName, filePrefix)
                }

                // For PDFs, copy as is
                if (mimeType.contains("pdf")) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = File(context.cacheDir, "${filePrefix}_${System.currentTimeMillis()}.pdf")

                    inputStream?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d(TAG, "$formName PDF size: ${file.length() / 1024} KB")

                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData(formName, file.name, requestFile)
                } else {
                    throw IllegalArgumentException("$formName harus berupa file PDF atau gambar (JPEG, JPG, PNG)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing $formName document", e)
                throw e
            }
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

    suspend fun sendFcm(request: FcmReq): FcmTokenResponse{
        return apiService.updateFcm(request)
    }

    suspend fun getListNotif(): Result<List<NotifItem>> {
        return try {
            val response = apiService.getNotif()

            if (response.isSuccessful){
                val chat = response.body()?.notif ?: emptyList()
                Result.Success(chat)
            } else {
                Result.Error(Exception("Failed to fetch list notif. Code: ${response.code()}"))
            }
        } catch (e: Exception){
            Result.Error(e)
        }
    }


    suspend fun getListNotifStore(): Result<List<NotifstoreItem>> {
        return try {
            val response = apiService.getNotifStore()

            if (response.isSuccessful){
                val chat = response.body()?.notifstore ?: emptyList()
                Result.Success(chat)
            } else {
                Result.Error(Exception("Failed to fetch list notif. Code: ${response.code()}"))
            }
        } catch (e: Exception){
            Result.Error(e)
        }
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