package com.alya.ecommerce_serang.data.api.retrofit

import com.alya.ecommerce_serang.data.api.response.AllProductResponse
import com.alya.ecommerce_serang.data.api.response.ProductResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {
    @GET("product")
    fun getAllProduct(
        @Header("Authorization") token: String = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6NzEsIm5hbWUiOiJhbHlhIiwiZW1haWwiOiJha3VuYmVsYWphci5hbHlhQGdtYWlsLmNvbSIsInJvbGUiOiJ1c2VyIiwiaWF0IjoxNzM4NDg0OTc0LCJleHAiOjE3NDEwNzY5NzR9.0JyXJQ_6CKiZEi0gvk1gcn-0ILu3a9lOr3HqjhJXbBE"
    ): Call<AllProductResponse>


    @GET("product/detail/{id}")
    fun getDetailProduct (
        @Header("Authorization") token: String,
        @Path("id") productId: Int
    ): Call<ProductResponse>
}