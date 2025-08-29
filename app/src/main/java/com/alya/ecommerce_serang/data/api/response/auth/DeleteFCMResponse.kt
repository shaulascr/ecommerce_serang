package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class DeleteFCMResponse(

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("user")
	val user: UserFCM
)

data class UserFCM(

	@field:SerializedName("name")
	val name: String,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("email")
	val email: String
)
