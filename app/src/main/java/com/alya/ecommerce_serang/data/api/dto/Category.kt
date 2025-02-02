package com.alya.ecommerce_serang.data.api.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String,
    val image: String,
    val title: String
): Parcelable