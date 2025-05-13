package com.alya.ecommerce_serang.ui.profile.mystore.profile.shipping_service

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ShippingServiceRepository
import com.alya.ecommerce_serang.databinding.ActivityShippingServiceBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ShippingServiceViewModel

class ShippingServiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShippingServiceBinding
    private lateinit var sessionManager: SessionManager
    private val courierCheckboxes = mutableListOf<Pair<CheckBox, String>>()

    private val viewModel: ShippingServiceViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val repository = ShippingServiceRepository(apiService)
            ShippingServiceViewModel(repository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShippingServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Configure header
        binding.header.headerTitle.text = "Atur Layanan Pengiriman"

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupCourierCheckboxes()
        setupObservers()

        binding.btnSave.setOnClickListener {
            saveShippingServices()
        }

        // Load shipping services
        viewModel.getAvailableCouriers()
    }

    private fun setupCourierCheckboxes() {
        // Add all courier checkboxes to the list for easy management
        courierCheckboxes.add(Pair(binding.checkboxJne, "jne"))
        courierCheckboxes.add(Pair(binding.checkboxPos, "pos"))
        courierCheckboxes.add(Pair(binding.checkboxTiki, "tiki"))
    }

    private fun setupObservers() {
        viewModel.availableCouriers.observe(this) { couriers ->
            // Check the appropriate checkboxes based on available couriers
            for (pair in courierCheckboxes) {
                val checkbox = pair.first
                val courierCode = pair.second
                checkbox.isChecked = couriers.contains(courierCode)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Layanan pengiriman berhasil disimpan", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun saveShippingServices() {
        val selectedCouriers = mutableListOf<String>()

        for (pair in courierCheckboxes) {
            val checkbox = pair.first
            val courierCode = pair.second
            if (checkbox.isChecked) {
                selectedCouriers.add(courierCode)
            }
        }

        if (selectedCouriers.isEmpty()) {
            Toast.makeText(this, "Pilih minimal satu layanan pengiriman", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveShippingServices(selectedCouriers)
    }
}