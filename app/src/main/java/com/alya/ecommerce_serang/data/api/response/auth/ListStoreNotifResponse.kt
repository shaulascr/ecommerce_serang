package com.alya.ecommerce_serang.data.api.response.auth

import com.google.gson.annotations.SerializedName

data class ListStoreNotifResponse(

	@field:SerializedName("notifstore")
	val notifstore: List<NotifstoreItem>,

	@field:SerializedName("message")
	val message: String
)

data class NotifstoreItem(

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
