package com.alya.ecommerce_serang.data.api.dto

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)