package com.alya.ecommerce_serang.ui.profile

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.UserProfile
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.MyStoreRepository
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.FragmentProfileBinding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
import com.alya.ecommerce_serang.ui.profile.mystore.RegisterStoreActivity
import com.alya.ecommerce_serang.ui.order.address.AddressActivity
import com.alya.ecommerce_serang.ui.order.history.HistoryActivity
import com.alya.ecommerce_serang.ui.profile.mystore.MyStoreActivity
import com.alya.ecommerce_serang.ui.profile.mystore.StoreOnReviewActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.MyStoreViewModel
import com.alya.ecommerce_serang.utils.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private val viewModel: ProfileViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val userRepository = UserRepository(apiService)
            ProfileViewModel(userRepository)
        }
    }

    private val myStoreViewModel: MyStoreViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val myStoreRepository = MyStoreRepository(apiService)
            MyStoreViewModel(myStoreRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserProfile()

        viewModel.loadUserProfile()
        viewModel.checkStoreUser()

        val hasStore = viewModel.checkStore.value
        Log.d("Profile Fragment", "Check store $hasStore")
        binding.tvBukaToko.text = if (hasStore == true) "Toko Saya" else "Buka Toko"

        binding.cardBukaToko.setOnClickListener{
//            if (hasStore == true) startActivity(Intent(requireContext(), MyStoreActivity::class.java))
//            else startActivity(Intent(requireContext(), RegisterStoreActivity::class.java))
            if (viewModel.checkStore.value == true) {
                myStoreViewModel.loadMyStore()
                myStoreViewModel.myStoreProfile.observe(viewLifecycleOwner) { store ->
                    store?.let {
                        when (store.storeStatus) {
                            "active" -> startActivity(Intent(requireContext(), MyStoreActivity::class.java))
                            else -> startActivity(Intent(requireContext(), StoreOnReviewActivity::class.java))
                        }
                    } ?: run {
                        Toast.makeText(requireContext(), "Gagal memuat data toko", Toast.LENGTH_SHORT).show()
                    }
                }
            } else startActivity(Intent(requireContext(), RegisterStoreActivity::class.java))
        }

        binding.btnDetailProfile.setOnClickListener{
            val intentDetail = Intent(requireContext(), DetailProfileActivity::class.java)
            startActivity(intentDetail)
        }

        binding.tvLihatRiwayat.setOnClickListener{
            val intent = Intent(requireContext(), HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.cardPesanan.setOnClickListener{
            val intent = Intent(requireContext(), HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.cardLogout.setOnClickListener{
            logout()
        }

        binding.cardAddress.setOnClickListener{
            val intent = Intent(requireContext(), AddressActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeUserProfile() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let { updateUI(it) }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(user: UserProfile) = with(binding){
        val fullImageUrl = when (val img = user.image) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image // Default image for null
        }

        Log.d("ProductAdapter", "Loading image: $fullImageUrl")

        tvName.text = user.name.toString()
        tvUsername.text = user.username.toString()

        // Load image using Glide
        Glide.with(requireContext())
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(profileImage)
    }

    private fun logout(){

        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                actionLogout()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun actionLogout(){
        val loadingDialog =  ProgressDialog(requireContext()).apply {
            setMessage("Mohon ditunggu")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                delay(500)
                loadingDialog.dismiss()
                sessionManager.clearAll()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal keluar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}