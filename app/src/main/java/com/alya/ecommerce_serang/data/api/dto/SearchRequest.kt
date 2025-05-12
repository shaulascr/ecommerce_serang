package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @SerializedName("search_query")
    val searchQuery: String
)