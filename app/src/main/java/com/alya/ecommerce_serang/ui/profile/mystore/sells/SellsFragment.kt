package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.FragmentSellsBinding
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.tabs.TabLayoutMediator

class SellsFragment : Fragment() {

    private var _binding: FragmentSellsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private lateinit var viewPagerAdapter: SellsViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupViewPager()
    }

    private fun setupViewPager() {
        // Initialize the ViewPager adapter
        viewPagerAdapter = SellsViewPagerAdapter(requireActivity())
        binding.viewPagerSells.adapter = viewPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayoutSells, binding.viewPagerSells) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.all_orders)           // "Semua Pesanan"
                1 -> getString(R.string.pending_orders)       // "Menunggu Tagihan"
                2 -> getString(R.string.unpaid_orders)        // "Konfirmasi Bayar"
                3 -> getString(R.string.paid_orders)          // "Diproses"
                4 -> getString(R.string.processed_orders)     // "Sudah Dibayar"
                5 -> getString(R.string.shipped_orders)       // "Dikirim"
                6 -> getString(R.string.completed_orders)     // "Selesai"
                7 -> getString(R.string.canceled_orders)      // "Dibatalkan"
                else -> "Tab $position"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}