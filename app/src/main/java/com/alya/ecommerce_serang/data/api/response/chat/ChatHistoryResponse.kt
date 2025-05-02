package com.alya.ecommerce_serang.data.api.response.chat

import com.google.gson.annotations.SerializedName

data class ChatHistoryResponse(

	@field:SerializedName("chat")
	val chat: List<ChatItem>,

	@field:SerializedName("message")
	val message: String
)

data class ChatItem(

	@field:SerializedName("attachment")
	val attachment: String? = null,

	@field:SerializedName("product_id")
	val productId: Int,

	@field:SerializedName("chat_room_id")
	val chatRoomId: Int,

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("id")
	val id: Int,

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("sender_id")
	val senderId: Int,

	@field:SerializedName("status")
	val status: String
)
