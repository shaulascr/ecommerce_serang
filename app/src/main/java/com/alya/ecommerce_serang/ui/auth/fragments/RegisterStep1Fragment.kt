package com.alya.ecommerce_serang.ui.auth.fragments

import android.app.DatePickerDialog
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
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.dto.VerifRegisReq
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.FragmentRegisterStep1Binding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
import com.alya.ecommerce_serang.ui.auth.RegisterActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.viewmodel.RegisterViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterStep1Fragment : Fragment() {
    private var _binding: FragmentRegisterStep1Binding? = null
    private val binding get() = _binding!!

    private val registerViewModel: RegisterViewModel by activityViewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val orderRepository = OrderRepository(apiService)
            val userRepository = UserRepository(apiService)
            RegisterViewModel(userRepository, orderRepository, requireContext())
        }
    }
    private var isEmailValid = false
    private var isPhoneValid = false

    companion object {
        private const val TAG = "RegisterStep1Fragment"

        fun newInstance() = RegisterStep1Fragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set step progress and description
        (activity as? RegisterActivity)?.let {
            it.findViewById<LinearProgressIndicator>(R.id.registration_progress)?.progress = 33
            it.findViewById<TextView>(R.id.tv_step_title)?.text = "Step 1: Informasi Akun"
            it.findViewById<TextView>(R.id.tv_step_description)?.text =
                "Masukkan data pengguna dengan data yang valid."
        }

        setupFieldValidations()
        setupObservers()
        setupDatePicker()

        binding.btnNext.setOnClickListener {
            validateAndProceed()
        }

        binding.tvLoginAlt.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun setupDatePicker() {
        binding.etBirthDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etBirthDate.setText(sdf.format(calendar.time))
            },
            year, month, day
        ).show()
    }

    private fun setupFieldValidations() {
        // Validate email when focus changes
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.etEmail.text.toString()
                if (email.isNotEmpty()) {
                    validateEmail(email)
                }
            }
        }

        // Validate phone when focus changes
        binding.etNumberPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val phone = binding.etNumberPhone.text.toString()
                if (phone.isNotEmpty()) {
                    validatePhone(phone)
                }
            }
        }
    }

    private fun validateEmail(email: String) {
        val checkValueEmail = VerifRegisReq(
            fieldRegis = "email",
            valueRegis = email
        )
        registerViewModel.checkValueReg(checkValueEmail)
    }

    private fun validatePhone(phone: String) {
        val checkValuePhone = VerifRegisReq(
            fieldRegis = "phone",
            valueRegis = phone
        )
        registerViewModel.checkValueReg(checkValuePhone)
    }

    private fun setupObservers() {
        registerViewModel.checkValue.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    // Show loading if needed
                }
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    val isValid = (result.data as? Boolean) ?: false
                    when (val fieldType = registerViewModel.lastCheckedField) {
                        "email" -> {
                            isEmailValid = isValid
                            if (!isValid) {
                                Toast.makeText(requireContext(), "Email is already registered", Toast.LENGTH_SHORT).show()
                            }
                        }
                        "phone" -> {
                            isPhoneValid = isValid
                            if (!isValid) {
                                Toast.makeText(requireContext(), "Phone number is already registered", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    Toast.makeText(requireContext(), "Validation failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        registerViewModel.otpState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnNext.isEnabled = false
                }
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnNext.isEnabled = true

                    // Create user data with both account and personal info
                    val userData = RegisterRequest(
                        name = binding.etFullname.text.toString(),
                        email = binding.etEmail.text.toString(),
                        password = binding.etPassword.text.toString(),
                        username = binding.etUsername.text.toString(),
                        phone = binding.etNumberPhone.text.toString(),
                        birthDate = binding.etBirthDate.text.toString(),
                        otp = ""  // Will be filled in step 2
                    )

                    registerViewModel.updateUserData(userData)
                    registerViewModel.setStep(2)
                    (activity as? RegisterActivity)?.navigateToStep(2, userData)
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnNext.isEnabled = true
                    Toast.makeText(requireContext(), "OTP Request Failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        registerViewModel.toastMessage.observe(viewLifecycleOwner){ event ->
            //memanggil toast check value email dan phone
            event.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateAndProceed() {
        // Validate account information
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val phone = binding.etNumberPhone.text.toString()
        val username = binding.etUsername.text.toString()

        // Validate personal information
        val fullName = binding.etFullname.text.toString()
        val birthDate = binding.etBirthDate.text.toString()
//        val gender = binding.etGender.text.toString()

        // Check if all fields are filled
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty() ||
            username.isEmpty() || fullName.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // If both validations are already done and successful, request OTP
        if (isEmailValid && isPhoneValid) {
            requestOtp(email)
            return
        }

        // Validate email and phone
        validateEmail(email)
        validatePhone(phone)

        // Only proceed if both are valid
        if (isEmailValid && isPhoneValid) {
            requestOtp(email)
        } else {
            Toast.makeText(requireContext(), "Please fix validation errors before proceeding", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOtp(email: String) {

        registerViewModel.requestOtp(email)

        registerViewModel.message.observe(viewLifecycleOwner) { message ->
            Log.d(TAG, "Message from server: $message")
            // You can use the message here if needed, e.g., for showing in a specific UI element
            // or for storing for later use
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}