package com.alya.ecommerce_serang.ui.order.address

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import com.alya.ecommerce_serang.data.api.response.customer.order.CitiesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.ProvincesItem
import com.alya.ecommerce_serang.data.api.response.customer.order.SubdistrictsItem
import com.alya.ecommerce_serang.data.api.response.customer.order.VillagesItem

// UI adapters and helpers
class ProvinceAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    //call from endpoint
    private val provinces = ArrayList<ProvincesItem>()

    fun updateData(newProvinces: List<ProvincesItem>) {
        provinces.clear()
        provinces.addAll(newProvinces)

        clear()
        addAll(provinces.map { it.province })
        notifyDataSetChanged()

        Log.d("ProvinceAdapter", "Updated with ${provinces.size} provinces")
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

    fun getCityId(position: Int): String? {
        return cities.getOrNull(position)?.cityId?.toString()
    }
}

class SubdsitrictAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    private val cities = ArrayList<SubdistrictsItem>()

    fun updateData(newCities: List<SubdistrictsItem>) {
        cities.clear()
        cities.addAll(newCities)

        clear()
        addAll(cities.map { it.subdistrictName })
        notifyDataSetChanged()
    }

    fun getSubdistrictId(position: Int): String? {
        return cities.getOrNull(position)?.subdistrictId?.toString()
    }
}

class VillagesAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    private val villages = ArrayList<VillagesItem>()

    fun updateData(newCities: List<VillagesItem>) {
        villages.clear()
        villages.addAll(newCities)

        clear()
        addAll(villages.map { it.villageName })
        notifyDataSetChanged()
    }

    fun getVillageId(position: Int): String? {
        return villages.getOrNull(position)?.villageId?.toString()
    }
    fun getPostalCode(position: Int): String?{
        return villages.getOrNull(position)?.postalCode
    }
}