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

//    companion object {
//        private const val TAG = "ProvinceAdapter"
//    }
//
//    // --- Static list of provinces ---------------------------------------------------------------
//    private val provinces = listOf(
//        ProvincesItem(1,  "Aceh"),
//        ProvincesItem(2,  "Sumatera Utara"),
//        ProvincesItem(3,  "Sumatera Barat"),
//        ProvincesItem(4,  "Riau"),
//        ProvincesItem(5,  "Kepulauan Riau"),
//        ProvincesItem(6,  "Jambi"),
//        ProvincesItem(7,  "Sumatera Selatan"),
//        ProvincesItem(8,  "Kepulauan Bangka Belitung"),
//        ProvincesItem(9,  "Bengkulu"),
//        ProvincesItem(10, "Lampung"),
//        ProvincesItem(11, "DKI Jakarta"),
//        ProvincesItem(12, "Jawa Barat"),
//        ProvincesItem(13, "Banten"),
//        ProvincesItem(14, "Jawa Tengah"),
//        ProvincesItem(15, "Daerah Istimewa Yogyakarta"),
//        ProvincesItem(16, "Jawa Timur"),
//        ProvincesItem(17, "Bali"),
//        ProvincesItem(18, "Nusa Tenggara Barat"),
//        ProvincesItem(19, "Nusa Tenggara Timur"),
//        ProvincesItem(20, "Kalimantan Barat"),
//        ProvincesItem(21, "Kalimantan Tengah"),
//        ProvincesItem(22, "Kalimantan Selatan"),
//        ProvincesItem(23, "Kalimantan Timur"),
//        ProvincesItem(24, "Kalimantan Utara"),
//        ProvincesItem(25, "Sulawesi Utara"),
//        ProvincesItem(26, "Gorontalo"),
//        ProvincesItem(27, "Sulawesi Tengah"),
//        ProvincesItem(28, "Sulawesi Barat"),
//        ProvincesItem(29, "Sulawesi Selatan"),
//        ProvincesItem(30, "Sulawesi Tenggara"),
//        ProvincesItem(31, "Maluku"),
//        ProvincesItem(32, "Maluku Utara"),
//        ProvincesItem(33, "Papua Barat"),
//        ProvincesItem(34, "Papua"),
//        ProvincesItem(35, "Papua Tengah"),
//        ProvincesItem(36, "Papua Pegunungan"),
//        ProvincesItem(37, "Papua Selatan"),
//        ProvincesItem(38, "Papua Barat Daya")
//    )
//
//    // --- Init block -----------------------------------------------------------------------------
//    init {
//        addAll(getProvinceNames())   // pre‑populate adapter
//        Log.d(TAG, "Adapter created with ${count} provinces")
//    }
//
//    // --- Public helper functions ----------------------------------------------------------------
//    fun updateData(newProvinces: List<ProvincesItem>) {
//        // If you actually want to replace the list, comment this line
//        // provinces = newProvinces               // (make `provinces` var instead of val)
//
//        clear()
//        addAll(newProvinces.map { it.province })
//        notifyDataSetChanged()
//
//        Log.d(TAG, "updateData(): updated with ${newProvinces.size} provinces")
//    }
//
//    fun getProvinceId(position: Int): Int? {
//        val id = provinces.getOrNull(position)?.provinceId
//        Log.d(TAG, "getProvinceId(): position=$position, id=$id")
//        return id
//    }
//
//    fun getProvinceItem(position: Int): ProvincesItem? {
//        val item = provinces.getOrNull(position)
//        Log.d(TAG, "getProvinceItem(): position=$position, item=$item")
//        return item
//    }
//
//    // --- Private helpers ------------------------------------------------------------------------
//    private fun getProvinceNames(): List<String> = provinces.map { it.province }

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