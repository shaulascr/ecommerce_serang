package com.alya.ecommerce_serang.data.api.response.customer.order

import com.google.gson.annotations.SerializedName

data class VillagesResponse(

	@field:SerializedName("villages")
	val villages: List<VillagesItem>,

	@field:SerializedName("message")
	val message: String
)

data class VillagesItem(

	@field:SerializedName("village_id")
	val villageId: String,

	@field:SerializedName("village_name")
	val villageName: String,

	@field:SerializedName("postal_code")
	val postalCode: String
)
