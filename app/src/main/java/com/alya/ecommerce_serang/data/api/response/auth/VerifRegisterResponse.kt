package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class VerifRegisterResponse(

	@field:SerializedName("available")
	val available: Boolean
)
