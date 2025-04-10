package com.alya.ecommerce_serang.data.api.response

import com.google.gson.annotations.SerializedName

data class CreateProductResponse(

	@field:SerializedName("product")
	val product: Product? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("preorder")
	val preorder: Preorder? = null
)

data class Preorder(

	@field:SerializedName("duration")
	val duration: Int? = null,

	@field:SerializedName("product_id")
	val productId: Int? = null,

	@field:SerializedName("id")
	val id: Int? = null
)
