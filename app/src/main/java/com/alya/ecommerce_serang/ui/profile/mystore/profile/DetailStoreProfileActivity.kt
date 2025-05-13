package com.alya.ecommerce_serang.ui.profile.mystore.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.Store
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.databinding.ActivityDetailStoreProfileBinding
import com.alya.ecommerce_serang.ui.profile.mystore.profile.address.DetailStoreAddressActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.payment_info.PaymentInfoActivity
import com.alya.ecommerce_serang.ui.profile.mystore.profile.shipping_service.ShippingServiceActivity
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import kotlin.getValue

class DetailStoreProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoreProfileBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private val viewModel: MyStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val myStoreRepository = MyStoreRepository(apiService)
            MyStoreViewModel(myStoreRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoreProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        // Set up header title
        binding.header.headerTitle.text = "Profil Toko"

        // Set up back button
        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnEditStoreProfile.setOnClickListener {
            val intent = Intent(this, EditStoreProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
        }

        binding.layoutAddress.setOnClickListener {
            val intent = Intent(this, DetailStoreAddressActivity::class.java)
            startActivityForResult(intent, ADDRESS_REQUEST_CODE)
        }

        // Set up payment method layout click listener
        binding.layoutPaymentMethod.setOnClickListener {
            val intent = Intent(this, PaymentInfoActivity::class.java)
            startActivityForResult(intent, PAYMENT_INFO_REQUEST_CODE)
        }

        // Set up shipping services layout click listener
        binding.layoutShipServices.setOnClickListener {
            val intent = Intent(this, ShippingServiceActivity::class.java)
            startActivityForResult(intent, SHIPPING_SERVICES_REQUEST_CODE)
        }

        viewModel.loadMyStore()

        viewModel.myStoreProfile.observe(this){ user ->
            user?.let { updateStoreProfile(it) }
        }

        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the profile data
            Toast.makeText(this, "Profil toko berhasil diperbarui", Toast.LENGTH_SHORT).show()
            viewModel.loadMyStore()

            // Pass the result back to parent activity
            setResult(Activity.RESULT_OK)
        } else if (requestCode == ADDRESS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the profile data after address update
            Toast.makeText(this, "Alamat toko berhasil diperbarui", Toast.LENGTH_SHORT).show()
            viewModel.loadMyStore()

            // Pass the result back to parent activity
            setResult(Activity.RESULT_OK)
        } else if (requestCode == PAYMENT_INFO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the profile data after payment method update
            Toast.makeText(this, "Metode pembayaran berhasil diperbarui", Toast.LENGTH_SHORT).show()
            viewModel.loadMyStore()

            // Pass the result back to parent activity
            setResult(Activity.RESULT_OK)
        } else if (requestCode == SHIPPING_SERVICES_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the profile data after shipping services update
            Toast.makeText(this, "Layanan pengiriman berhasil diperbarui", Toast.LENGTH_SHORT).show()
            viewModel.loadMyStore()

            // Pass the result back to parent activity
            setResult(Activity.RESULT_OK)
        }
    }

    companion object {
        private const val EDIT_PROFILE_REQUEST_CODE = 100
        private const val ADDRESS_REQUEST_CODE = 101
        private const val PAYMENT_INFO_REQUEST_CODE = 102
        private const val SHIPPING_SERVICES_REQUEST_CODE = 103
    }

    private fun updateStoreProfile(store: Store){
        // Update text fields
        binding.edtNamaToko.setText(store.storeName.toString())
        binding.edtJenisToko.setText(store.storeType.toString())
        binding.edtDeskripsiToko.setText(store.storeDescription.toString())

        // Update store image if available
        if (store.storeImage != null && store.storeImage.toString().isNotEmpty() && store.storeImage.toString() != "null") {
            val imageUrl = "http://192.168.100.156:3000${store.storeImage}"
            Log.d("DetailStoreProfile", "Loading image from: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.ivProfile)
        } else {
            Log.d("DetailStoreProfile", "No store image available")
        }
    }
}