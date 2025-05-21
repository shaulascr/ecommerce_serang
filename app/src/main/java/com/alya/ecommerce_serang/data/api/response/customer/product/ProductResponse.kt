package com.alya.ecommerce_serang.data.api.response.customer.product

import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
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

	@field:SerializedName("is_wholesale")
	val isWholesale: Boolean? = false,

	@field:SerializedName("sppirt")
	val sppirt: String? = null,

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

	@field:SerializedName("condition")
	val condition: String? = null,

	@field:SerializedName("category_id")
	val categoryId: Int,

	@field:SerializedName("price")
	val price: String,

	@field:SerializedName("product_id")
	val productId: Int,

	@field:SerializedName("wholesale_price")
	val wholesalePrice: String? = null,

	@field:SerializedName("halal")
	val halal: String? = null,

	@field:SerializedName("wholesale_min_item")
	val wholesaleMinItem: Int? = null,

	@field:SerializedName("min_order")
	val minOrder: Int,

	@field:SerializedName("total_sold")
	val totalSold: Int,

	@field:SerializedName("stock")
	val stock: Int,

	@field:SerializedName("product_category")
	val productCategory: String,

	@field:SerializedName("preorder_duration")
	val preorderDuration: String? = null
)

data class CartItemWholesaleInfo(
	val cartItemId: Int,
	val isWholesale: Boolean,
	val wholesalePrice: Double? = null
)

data class CartItemCheckoutInfo(
	val cartItem: CartItemsItem,
	val isWholesale: Boolean
)
