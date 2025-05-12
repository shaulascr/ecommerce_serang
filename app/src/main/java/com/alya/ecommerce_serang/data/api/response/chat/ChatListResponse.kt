package com.alya.ecommerce_serang.data.api.response.chat

import com.google.gson.annotations.SerializedName

data class ChatListResponse(

	@field:SerializedName("chat")
	val chat: List<ChatItemList>,

	@field:SerializedName("message")
	val message: String
)

data class ChatItemList(

	@field:SerializedName("store_id")
	val storeId: Int,

	@field:SerializedName("user_id")
	val userId: Int,

	@field:SerializedName("user_image")
	val userImage: String? = null,

	@field:SerializedName("user_name")
	val userName: String,

	@field:SerializedName("chat_room_id")
	val chatRoomId: Int,

	@field:SerializedName("latest_message_time")
	val latestMessageTime: String,

	@field:SerializedName("store_name")
	val storeName: String,

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("store_image")
	val storeImage: String? = null
)
