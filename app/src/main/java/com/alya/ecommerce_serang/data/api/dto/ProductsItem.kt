package com.alya.ecommerce_serang.data.api.dto


import com.google.gson.annotations.SerializedName

data class ProductsItem(

    @field:SerializedName("store_id")
    val storeId: Int,

    @field:SerializedName("image")
    val image: String,

    @field:SerializedName("is_wholesale")
    val isWholesale: Boolean,

    @field:SerializedName("sppirt")
    val sppirt: String? = null,

    @field:SerializedName("rating")
    val rating: String,

    @field:SerializedName("description")
    val description: String,

    @field:SerializedName("weight")
    val weight: Int,

    @field:SerializedName("is_pre_order")
    val isPreOrder: Boolean,

    @field:SerializedName("condition")
    val condition: String? = null,

    @field:SerializedName("category_id")
    val categoryId: Int,

    @field:SerializedName("price")
    val price: String,

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("halal")
    val halal: String?= null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("min_order")
    val minOrder: Int,

    @field:SerializedName("total_sold")
    val totalSold: Int,

    @field:SerializedName("stock")
    val stock: Int,

    @field:SerializedName("status")
    val status: String
)
