package com.alya.ecommerce_serang.data.api.response.product

import com.google.gson.annotations.SerializedName

data class CreateSearchResponse(

	@field:SerializedName("search")
	val search: Search
)

data class Search(

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("search_query")
	val searchQuery: String
)
