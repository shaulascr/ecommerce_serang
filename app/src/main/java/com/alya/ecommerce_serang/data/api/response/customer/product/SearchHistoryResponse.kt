package com.alya.ecommerce_serang.data.api.response.product

import com.google.gson.annotations.SerializedName

data class SearchHistoryResponse(

	@field:SerializedName("data")
	val data: List<DataItem>
)

data class DataItem(

	@field:SerializedName("search_query")
	val searchQuery: String
)
