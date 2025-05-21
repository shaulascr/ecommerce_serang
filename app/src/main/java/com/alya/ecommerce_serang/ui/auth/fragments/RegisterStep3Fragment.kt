package com.alya.ecommerce_serang.ui.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CreateAddressRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.FragmentRegisterStep3Binding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
import com.alya.ecommerce_serang.ui.auth.RegisterActivity
import com.alya.ecommerce_serang.ui.order.address.CityAdapter
import com.alya.ecommerce_serang.ui.order.address.ProvinceAdapter
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.RegisterViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator

class RegisterStep3Fragment : Fragment() {
    private var _binding: FragmentRegisterStep3Binding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private val defaultLatitude = -6.200000
    private val defaultLongitude = 106.816666

    // In RegisterStep2Fragment AND RegisterStep3Fragment:
    private val registerViewModel: RegisterViewModel by activityViewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val orderRepository = OrderRepository(apiService)
            val userRepository = UserRepository(apiService)
            RegisterViewModel(userRepository, orderRepository, requireContext())
        }
    }
    // For province and city selection
    private val provinceAdapter by lazy { ProvinceAdapter(requireContext()) }
    private val cityAdapter by lazy { CityAdapter(requireContext()) }

    companion object {
        private const val TAG = "RegisterStep3Fragment"

        fun newInstance() = RegisterStep3Fragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        Log.d(TAG, "SessionManager initialized, token: ${sessionManager.getToken()}")

        // Set step progress and description
        (activity as? RegisterActivity)?.let {
            it.findViewById<LinearProgressIndicator>(R.id.registration_progress)?.progress = 100
            it.findViewById<TextView>(R.id.tv_step_title)?.text = "Step 3: Tambahkan Alamat"
            it.findViewById<TextView>(R.id.tv_step_description)?.text =
                "Masukkan alamat untuk menerima pesanan."
            Log.d(TAG, "Step indicators updated to Step 1")
        }

        // Get registered user data
        val user = registerViewModel.registeredUser.value
        Log.d(TAG, "Retrieved user data: ${user?.name}, ID: ${user?.id}")

        // Auto-fill recipient name and phone if available
        user?.let {
            binding.etNamaPenerima.setText(it.name)
            binding.etNomorHp.setText(it.phone)
            Log.d(TAG, "Auto-filled name: ${it.name}, phone: ${it.phone}")
        }

        // Set up province and city dropdowns
        setupAutoComplete()

        // Set up button listeners
        binding.btnPrevious.setOnClickListener {
            // Go back to the previous step
            parentFragmentManager.popBackStack()
        }

        binding.btnRegister.setOnClickListener {
            submitAddress()
        }

        // If user skips address entry
//        binding.btnSkip.setOnClickListener {
//            showRegistrationSuccess()
//        }

        // Observe address submission state
        observeAddressSubmissionState()

        // Load provinces
        Log.d(TAG, "Requesting provinces data")
        registerViewModel.getProvinces()
        setupProvinceObserver()
        setupCityObserver()
    }

    private fun setupAutoComplete() {
        // Same implementation as before
        binding.autoCompleteProvinsi.setAdapter(provinceAdapter)
        binding.autoCompleteKabupaten.setAdapter(cityAdapter)

        binding.autoCompleteProvinsi.setOnClickListener {
            binding.autoCompleteProvinsi.showDropDown()
        }

        binding.autoCompleteKabupaten.setOnClickListener {
            if (cityAdapter.count > 0) {
                Log.d(TAG, "City dropdown clicked, showing ${cityAdapter.count} cities")
                binding.autoCompleteKabupaten.showDropDown()
            } else {
                Toast.makeText(requireContext(), "Pilih provinsi terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        binding.autoCompleteProvinsi.setOnItemClickListener { _, _, position, _ ->
            val provinceId = provinceAdapter.getProvinceId(position)
            Log.d(TAG, "Province selected at position $position, ID: $provinceId")

            provinceId?.let { id ->
                registerViewModel.selectedProvinceId = id
                Log.d(TAG, "Requesting cities for province ID: $id")
                registerViewModel.getCities(id)
                binding.autoCompleteKabupaten.text.clear()
            }
        }

        binding.autoCompleteKabupaten.setOnItemClickListener { _, _, position, _ ->
            val cityId = cityAdapter.getCityId(position)
            Log.d(TAG, "City selected at position $position, ID: $cityId")

            cityId?.let { id ->
                Log.d(TAG, "Selected city ID set to: $id")
                registerViewModel.selectedCityId = id
            }
        }
    }

    private fun setupProvinceObserver() {
        // Same implementation as before
        registerViewModel.provincesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ViewState.Loading -> {
                    binding.progressBarProvinsi.visibility = View.VISIBLE
                }
                is ViewState.Success -> {
                    Log.d(TAG, "Provinces: Success - received ${state.data.size} provinces")
                    binding.progressBarProvinsi.visibility = View.GONE
                    if (state.data.isNotEmpty()) {
                        provinceAdapter.updateData(state.data)
                    } else {
                        showError("No provinces available")
                    }
                }
                is ViewState.Error -> {
                    Log.e(TAG, "Provinces: Error - ${state.message}")
                    binding.progressBarProvinsi.visibility = View.GONE
                    showError("Failed to load provinces: ${state.message}")
                }
            }
        }
    }

    private fun setupCityObserver() {
        // Same implementation as before
        registerViewModel.citiesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ViewState.Loading -> {
                    binding.progressBarKabupaten.visibility = View.VISIBLE
                }
                is ViewState.Success -> {
                    Log.d(TAG, "Cities: Success - received ${state.data.size} cities")
                    binding.progressBarKabupaten.visibility = View.GONE
                    cityAdapter.updateData(state.data)
                    Log.d(TAG, "Updated city adapter with ${state.data.size} items")
                }
                is ViewState.Error -> {
                    Log.e(TAG, "Cities: Error - ${state.message}")
                    binding.progressBarKabupaten.visibility = View.GONE
                    showError("Failed to load cities: ${state.message}")
                }
            }
        }
    }

    private fun submitAddress() {
        Log.d(TAG, "submitAddress called")
        if (!validateAddressForm()) {
            Log.w(TAG, "Address form validation failed")
            return
        }

        val user = registerViewModel.registeredUser.value
        if (user == null) {
            Log.e(TAG, "User data not available")
            showError("User data not available. Please try again.")
            return
        }

        val userId = user.id
        Log.d(TAG, "Using user ID: $userId")

        val street = binding.etDetailAlamat.text.toString().trim()
        val subDistrict = binding.etKecamatan.text.toString().trim()
        val postalCode = binding.etKodePos.text.toString().trim()
        val recipient = binding.etNamaPenerima.text.toString().trim()
        val phone = binding.etNomorHp.text.toString().trim()

        val provinceId = registerViewModel.selectedProvinceId?.toInt() ?: 0
        val cityId = registerViewModel.selectedCityId?.toInt() ?: 0

        Log.d(TAG, "Address data - Street: $street, SubDistrict: $subDistrict, PostalCode: $postalCode")
        Log.d(TAG, "Address data - Recipient: $recipient, Phone: $phone")
        Log.d(TAG, "Address data - ProvinceId: $provinceId, CityId: $cityId")
        Log.d(TAG, "Address data - Lat: $defaultLatitude, Long: $defaultLongitude")

        // Create address request
        val addressRequest = CreateAddressRequest(
            lat = defaultLatitude,
            long = defaultLongitude,
            street = street,
            subDistrict = subDistrict,
            cityId = cityId,
            provId = provinceId,
            postCode = postalCode,
            detailAddress = street,
            userId = userId,
            recipient = recipient,
            phone = phone,
            isStoreLocation = false
        )

        Log.d(TAG, "Address request created: $addressRequest")

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
//        binding.btnSkip.isEnabled = false

        // Submit address
        registerViewModel.addAddress(addressRequest)
    }

    private fun validateAddressForm(): Boolean {
        val street = binding.etDetailAlamat.text.toString().trim()
        val subDistrict = binding.etKecamatan.text.toString().trim()
        val postalCode = binding.etKodePos.text.toString().trim()
        val recipient = binding.etNamaPenerima.text.toString().trim()
        val phone = binding.etNomorHp.text.toString().trim()

        val provinceId = registerViewModel.selectedProvinceId
        val cityId = registerViewModel.selectedCityId

        Log.d(TAG, "Validating - Street: $street, SubDistrict: $subDistrict, PostalCode: $postalCode")
        Log.d(TAG, "Validating - Recipient: $recipient, Phone: $phone")
        Log.d(TAG, "Validating - ProvinceId: $provinceId, CityId: $cityId")


        // Validate required fields
        if (street.isBlank()) {
            binding.etDetailAlamat.error = "Alamat tidak boleh kosong"
            binding.etDetailAlamat.requestFocus()
            return false
        }

        if (recipient.isBlank()) {
            binding.etNamaPenerima.error = "Nama penerima tidak boleh kosong"
            binding.etNamaPenerima.requestFocus()
            return false
        }

        if (phone.isBlank()) {
            binding.etNomorHp.error = "Nomor HP tidak boleh kosong"
            binding.etNomorHp.requestFocus()
            return false
        }

        if (provinceId == null) {
            showError("Pilih provinsi terlebih dahulu")
            binding.autoCompleteProvinsi.requestFocus()
            return false
        }

        if (cityId == null) {
            showError("Pilih kota/kabupaten terlebih dahulu")
            binding.autoCompleteKabupaten.requestFocus()
            return false
        }

        return true
    }

    private fun observeAddressSubmissionState() {
        registerViewModel.addressSubmissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ViewState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
//                    binding.btnSkip.isEnabled = false
                }
                is ViewState.Success -> {
                    Log.d(TAG, "Address submission: Success - ${state.data}")
                    binding.progressBar.visibility = View.GONE
                    showRegistrationSuccess()
                }
                is ViewState.Error -> {
                    Log.e(TAG, "Address submission: Error - ${state.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
//                    binding.btnSkip.isEnabled = true
                    showError("Failed to add address: ${state.message}")
                }
            }
        }
    }

    private fun showRegistrationSuccess() {
        // Now we can show the success message for the overall registration process
        Toast.makeText(requireContext(), "Registration completed successfully!", Toast.LENGTH_LONG).show()

        // Navigate to login screen
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        Log.d(TAG, "Navigating to LoginActivity")
        activity?.finish()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
//
//    // Data classes for province and city
//    data class Province(val id: String, val name: String)
//    data class City(val id: String, val name: String)
}