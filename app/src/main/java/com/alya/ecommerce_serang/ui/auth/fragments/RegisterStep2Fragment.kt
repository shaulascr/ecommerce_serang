package com.alya.ecommerce_serang.ui.auth.fragments

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.FcmReq
import com.alya.ecommerce_serang.data.api.dto.RegisterRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.FragmentRegisterStep2Binding
import com.alya.ecommerce_serang.ui.auth.RegisterActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.RegisterViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.messaging.FirebaseMessaging

class RegisterStep2Fragment : Fragment() {
    private var _binding: FragmentRegisterStep2Binding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining = 30
    private var isTimerRunning = false

    // In RegisterStep2Fragment AND RegisterStep3Fragment:
    private val registerViewModel: RegisterViewModel by activityViewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getUnauthenticatedApiService()
            val orderRepository = OrderRepository(apiService)
            val userRepository = UserRepository(apiService)
            RegisterViewModel(userRepository, orderRepository, requireContext())
        }
    }
//    private var countDownTimer: CountDownTimer? = null
//    private var timeRemaining = 30 // 30 seconds cooldown for resend

    companion object {

        private const val TAG = "RegisterStep2Fragment"
        fun newInstance(userData: RegisterRequest?) = RegisterStep2Fragment().apply {
            arguments = Bundle().apply {
                putParcelable("userData", userData)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        Log.d(TAG, "SessionManager initialized, token: ${sessionManager.getToken()}")

        // Set step progress and description
        (activity as? RegisterActivity)?.let {
            it.findViewById<LinearProgressIndicator>(R.id.registration_progress)?.progress = 66
            it.findViewById<TextView>(R.id.tv_step_title)?.text = "Step 2: Masukkan Kode OTP"
            it.findViewById<TextView>(R.id.tv_step_description)?.text =
                "Cek email untuk melihat kode OTP. Pastikan email yang digunakan aktif."
            Log.d(TAG, "Step indicators updated to Step 2")
        }


        // Get the user data from arguments
        val userData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("userData", RegisterRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("userData") as? RegisterRequest
        }
        Log.d(TAG, "User data retrieved from arguments: ${userData?.email}, ${userData?.name}")

        // Update the email sent message
        userData?.let {
            binding.tvEmailSent.text = "Kami telah mengirimkan kode OTP ke alamat email ${it.email}. Silahkan periksa email anda."
        }

        // Start the resend cooldown timer
        startResendCooldown()
        Log.d(TAG, "Resend cooldown timer started")

        // Set up button listeners
        binding.btnVerify.setOnClickListener {
            verifyOtp(userData)
        }

        binding.tvResendOtp.setOnClickListener {
            if (timeRemaining <= 0) {
                Log.d(TAG, "Resend OTP clicked, remaining time: $timeRemaining")
                resendOtp(userData?.email)
            } else {
                Log.d(TAG, "Resend OTP clicked but cooldown active, remaining time: $timeRemaining")
            }
        }

        observeRegistrationState()
        observeLoginState()
        Log.d(TAG, "Registration and login state observers set up")
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked - cleaning up timer and going to step 1")

            // Stop the timer before navigating
            stopTimer()
            // Small delay to ensure timer is properly canceled
            binding.root.postDelayed({
//                (activity as? RegisterActivity)?.navigateToStep(1, null)
                val intent = Intent(requireContext(), RegisterActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

            }, 100)
        }
    }

    private fun verifyOtp(userData: RegisterRequest?) {
        val otp = binding.etOtp.text.toString()
        Log.d(TAG, "verifyOtp called with OTP: $otp")

        if (otp.isEmpty()) {
            Toast.makeText(requireContext(), "Masukkan kode OTP anda", Toast.LENGTH_SHORT).show()
            return
        }

        // Update the user data with the OTP
        userData?.let {
            val updatedUserData = it.copy(otp = otp)
            Log.d(TAG, "Updating user data with OTP: $otp")
            registerViewModel.updateUserData(updatedUserData)

            registerViewModel.registerUser(updatedUserData)
        }  ?: Log.e(TAG, "userData is null, cannot proceed with verification")
    }

    private fun resendOtp(email: String?) {
        Log.d(TAG, "resendOtp called for email: $email")
        email?.let {
            binding.progressBar.visibility = View.VISIBLE
            Log.d(TAG, "Requesting OTP for: $it")
            registerViewModel.requestOtp(it)

            // Observe the OTP state
            registerViewModel.otpState.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is com.alya.ecommerce_serang.data.repository.Result.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Kode OTP sudah dikirim", Toast.LENGTH_SHORT).show()
                        startResendCooldown()
                    }
                    is Result.Error -> {
                        Log.e(TAG, "OTP request: Error - ${result.exception.message}")
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Gagal mengirim kode OTP", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.d(TAG, "OTP request: Unknown state")
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        } ?: Log.e(TAG, "Cannot resend OTP: email is null")
    }

    private fun observeRegistrationState() {
        registerViewModel.message.observe(viewLifecycleOwner) { message ->
            Log.d(TAG, "Message from server: $message")
        }
        registerViewModel.registerState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnVerify.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "Registration: Success - ${result.data}")
                    // Don't hide progress bar or re-enable button yet
                    // We'll wait for login to complete

                    // Don't show success toast yet - wait until address is added
                    Log.d("RegisterStep2Fragment", "Registration successful, waiting for login")
                }
                is Result.Error -> {
                    Log.e(TAG, "Registration: Error - ${result.exception.message}", result.exception)
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true

                    // Show error message
                    Log.e(TAG, "Registration Failed: ${result.exception.message}")
                    Toast.makeText(requireContext(), "Gagal melakukan regsitrasi", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.d(TAG, "Registration: Unknown state")
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                }
            }
        }
    }

    private fun observeLoginState() {
        registerViewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Keep showing progress
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnVerify.isEnabled = false
                }
                is Result.Success -> {
                    Log.d(TAG, "Login: Success - token received")
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true

                    // Save the token in fragment
                    val accessToken = result.data.accessToken
                    sessionManager.saveToken(accessToken)
                    retrieveFCMToken()

                    Log.d(TAG, "Token saved to SessionManager: $accessToken")

                    // Proceed to Step 3
                    Log.d(TAG, "Proceeding to Step 3 after successful login")

                    // call navigate register step from activity
                    (activity as? RegisterActivity)?.navigateToStep(3, null )
                }
                is Result.Error -> {
                    Log.e(TAG, "Login: Error - ${result.exception.message}", result.exception)
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true

                    // Show error message but continue to Step 3 anyway
                    Log.e(TAG, "Login failed but proceeding to Step 3", result.exception)
                    Toast.makeText(requireContext(), "Berhasil membuat akun, namun belum login", Toast.LENGTH_SHORT).show()

                    // Proceed to Step 3
                    (activity as? RegisterActivity)?.navigateToStep(3, null)
                }
                else -> {
                    Log.d(TAG, "Login: Unknown state")
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                }
            }
        }
    }

    private fun retrieveFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
//                tokenTes = token
                Log.d(TAG, "FCM token retrieved: $token")

                // Save token locally
                val sharedPreferences = requireContext().getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("FCM_TOKEN", token).apply()

                // Send to your server
                sendTokenToServer(token)
            }
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "Would send token to server: $token")
        val tokenFcm=FcmReq(
            fcmToken = token
        )
        registerViewModel.sendFcm(tokenFcm)
        Log.d(TAG, "Sent token fcm: $token")

    }

    private fun startResendCooldown() {
        Log.d(TAG, "startResendCooldown called")

        // Cancel any existing timer first
        stopTimer()

        timeRemaining = 30
        isTimerRunning = true
        binding.tvResendOtp.isEnabled = false
        binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.soft_gray))

        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isTimerRunning) {
                    cancel()
                    return
                }

                timeRemaining = (millisUntilFinished / 1000).toInt()

                // Check if fragment is still attached before updating UI
                if (isAdded && _binding != null) {
                    binding.tvTimer.text = "Kirim ulang OTP dalam waktu 00:${String.format("%02d", timeRemaining)}"
                    if (timeRemaining % 5 == 0) {
                        Log.d(TAG, "Cooldown remaining: $timeRemaining seconds")
                    }
                }
            }

            override fun onFinish() {
                if (!isTimerRunning) return

                Log.d(TAG, "Cooldown finished, enabling resend button")

                // Check if fragment is still attached before updating UI
                if (isAdded && _binding != null) {
                    binding.tvTimer.text = "Dapat mengirim ulang kode OTP"
                    binding.tvResendOtp.isEnabled = true
                    binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue1))
                    timeRemaining = 0
                }
                isTimerRunning = false
            }
        }.start()
    }

    private fun stopTimer() {
        Log.d(TAG, "stopTimer called")
        isTimerRunning = false
        countDownTimer?.cancel()
        countDownTimer = null
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - stopping timer")
        stopTimer()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop - stopping timer")
        stopTimer()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView - cleaning up")
        super.onDestroyView()

        // Ensure timer is stopped
        stopTimer()

        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach - final cleanup")
        stopTimer()
    }
}

//    override fun onDestroyView() {
//        super.onDestroyView()
//        countDownTimer?.cancel()
//        _binding = null
//    }

//    private fun startResendCooldown() {
//        Log.d(TAG, "startResendCooldown called")
//        timeRemaining = 30
//        binding.tvResendOtp.isEnabled = false
//        binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.soft_gray))
//
//        countDownTimer?.cancel()
//        countDownTimer = object : CountDownTimer(30000, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                timeRemaining = (millisUntilFinished / 1000).toInt()
//                binding.tvTimer.text = "Kirim ulang OTP dalam waktu 00:${String.format("%02d", timeRemaining)}"
//                if (timeRemaining % 5 == 0) {
//                    Log.d(TAG, "Cooldown remaining: $timeRemaining seconds")
//                }
//            }
//
//            override fun onFinish() {
//                Log.d(TAG, "Cooldown finished, enabling resend button")
//                binding.tvTimer.text = "Dapat mengirim ulang kode OTP"
//                binding.tvResendOtp.isEnabled = true
//                binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue1))
//                timeRemaining = 0
//            }
//        }.start()
//    }