package com.alya.ecommerce_serang.ui.order.address

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.alya.ecommerce_serang.R
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

    fun getProvinceName(position: Int): String? {
        return provinces.getOrNull(position)?.province?.toString()
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

    fun getCityName(position: Int): String? {
        return cities.getOrNull(position)?.cityName?.toString()
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

    fun getSubdistrictName(position: Int): String? {
        return cities.getOrNull(position)?.subdistrictName?.toString()
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

    fun getVillageName(position: Int): String? {
        return villages.getOrNull(position)?.villageName.toString()
    }
    fun getPostalCode(position: Int): String?{
        return villages.getOrNull(position)?.postalCode
    }
}

class BankAdapter(
    context: Context,
    resource: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<String>(context, resource, ArrayList()) {

    data class BankItem(
        val bankName: String,
        val bankCode: String? = null,
        val description: String? = null
    )

    private val banks = ArrayList<BankItem>()

    init {
        loadHardcodedData()
    }

    private fun loadHardcodedData() {
        val bankNames = context.resources.getStringArray(R.array.bank_names)
        val defaultBanks = bankNames.map { BankItem(bankName = it) }
        updateData(defaultBanks)
    }

    fun updateData(newBanks: List<BankItem>) {
        banks.clear()
        banks.addAll(newBanks)

        clear()
        addAll(banks.map { it.bankName })
        notifyDataSetChanged()
    }

    fun getBankName(position: Int): String? {
        return banks.getOrNull(position)?.bankName
    }

    fun getBankItem(position: Int): BankItem? {
        return banks.getOrNull(position)
    }

    fun getBankCode(position: Int): String? {
        return banks.getOrNull(position)?.bankCode
    }

    fun findPositionByName(bankName: String): Int {
        return banks.indexOfFirst { it.bankName == bankName }
    }

    fun setDefaultSelection(spinner: Spinner, defaultBankName: String) {
        val position = findPositionByName(defaultBankName)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }
}