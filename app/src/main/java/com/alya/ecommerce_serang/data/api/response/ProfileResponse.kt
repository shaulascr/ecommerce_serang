package com.alya.ecommerce_serang.data.api.response

import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.google.gson.annotations.SerializedName

data class ProfileResponse(

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("user")
	val user: UserProfile
)


