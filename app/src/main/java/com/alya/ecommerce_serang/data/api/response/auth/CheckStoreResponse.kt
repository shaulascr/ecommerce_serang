package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class CheckStoreResponse(

	@field:SerializedName("hasStore")
	val hasStore: Boolean
)
