package com.alya.ecommerce_serang.ui.order

import android.content.Context
import android.widget.ArrayAdapter
import com.alya.ecommerce_serang.data.api.response.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.order.ProvincesItem

// UI adapters and helpers
class ProvinceAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    private val provinces = ArrayList<ProvincesItem>()

    fun updateData(newProvinces: List<ProvincesItem>) {
        provinces.clear()
        provinces.addAll(newProvinces)

        clear()
        addAll(provinces.map { it.province })
        notifyDataSetChanged()
    }

    fun getProvinceId(position: Int): Int? {
        return provinces.getOrNull(position)?.provinceId?.toIntOrNull()
    }
}

class CityAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    private val cities = ArrayList<CitiesItem>()

    fun updateData(newCities: List<CitiesItem>) {
        cities.clear()
        cities.addAll(newCities)

        clear()
        addAll(cities.map { it.cityName })
        notifyDataSetChanged()
    }

    fun getCityId(position: Int): Int? {
        return cities.getOrNull(position)?.cityId?.toIntOrNull()
    }
}