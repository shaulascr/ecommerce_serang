package com.alya.ecommerce_serang.data.api.response

import com.google.gson.annotations.SerializedName

data class ProductResponse(

	@field:SerializedName("product")
	val product: Product,

	@field:SerializedName("message")
	val message: String
)

data class Product(

	@field:SerializedName("store_id")
	val storeId: Int,

	@field:SerializedName("image")
	val image: String,

	@field:SerializedName("rating")
	val rating: String,

	@field:SerializedName("description")
	val description: String,

	@field:SerializedName("weight")
	val weight: Int,

	@field:SerializedName("product_name")
	val productName: String,

	@field:SerializedName("is_pre_order")
	val isPreOrder: Boolean,

	@field:SerializedName("duration")
	val duration: Any?,

	@field:SerializedName("category_id")
	val categoryId: Int,

	@field:SerializedName("price")
	val price: String,

	@field:SerializedName("product_id")
	val productId: Int,

	@field:SerializedName("min_order")
	val minOrder: Int,

	@field:SerializedName("total_sold")
	val totalSold: Int,

	@field:SerializedName("stock")
	val stock: Int,

	@field:SerializedName("product_category")
	val productCategory: String
)
