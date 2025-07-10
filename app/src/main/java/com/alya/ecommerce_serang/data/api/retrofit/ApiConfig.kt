package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.BuildConfig
import com.alya.ecommerce_serang.utils.AuthInterceptor
import com.alya.ecommerce_serang.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiConfig {
    companion object {
        fun getApiService(tokenManager: SessionManager): ApiService {

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                //httplogginginterceptor ntuk debug dan monitoring request/response
            }

            val authInterceptor = AuthInterceptor(tokenManager)
            // utk tambak token auth otomatis pada header

            // Konfigurasi OkHttpClient
            //Low-level HTTP client yang melakukan actual network request
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(180, TimeUnit.SECONDS)  // 3 minutes
                .readTimeout(300, TimeUnit.SECONDS)     // 5 minutes
                .writeTimeout(300, TimeUnit.SECONDS)    // 5 minutes
                .build()

            // Konfigurasi Retrofit
            val retrofit = Retrofit.Builder()
                //almat domain backend
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                //gson convertes: mengkonversi JSON ke object Kotlin dan sebaliknya
                .client(client)
                .build()

            return retrofit.create(ApiService::class.java)
            // retrofit : menyederhanakan HTTP Request dgn mengubah interface Kotlin di ApiService menjadi HTTP calls secara otomatis
        }

        fun getUnauthenticatedApiService(): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}