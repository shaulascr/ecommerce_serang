package com.alya.ecommerce_serang.data.api.response

import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.google.gson.annotations.SerializedName

data class AllProductResponse(

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("products")
	val products: List<ProductsItem>
)


