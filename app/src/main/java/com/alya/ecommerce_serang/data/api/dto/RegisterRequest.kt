package com.alya.ecommerce_serang.data.api.dto

data class RegisterRequest (
    val name: String?,
    val email: String?,
    val password: String?,
    val username: String?,
    val phone: String?,
    val birthDate: String?,
    val image: String?,
    val otp: String? = null
)