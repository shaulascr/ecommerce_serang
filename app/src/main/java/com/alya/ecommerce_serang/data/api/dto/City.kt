package com.alya.ecommerce_serang.data.api.dto

import com.google.gson.annotations.SerializedName

data class CityResponse(

	@field:SerializedName("cities")
	val cities: List<City>,

	@field:SerializedName("message")
	val message: String
)

data class City(

	@field:SerializedName("city_name")
	val cityName: String,

	@field:SerializedName("city_id")
	val cityId: String
)
