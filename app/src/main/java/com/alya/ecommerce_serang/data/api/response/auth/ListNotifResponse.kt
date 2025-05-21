package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class ListNotifResponse(

	@field:SerializedName("notif")
	val notif: List<NotifItem>,

	@field:SerializedName("message")
	val message: String
)

data class NotifItem(

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("title")
	val title: String,

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("type")
	val type: String
)
