package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.material.tabs.TabLayoutMediator
import com.alya.ecommerce_serang.databinding.FragmentSellsBinding
import com.alya.ecommerce_serang.utils.SessionManager

class SellsFragment : Fragment() {

    private var _binding: FragmentSellsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private lateinit var viewPagerAdapter: SellsViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSellsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        viewPagerAdapter = SellsViewPagerAdapter(requireActivity())
        binding.viewPagerSells.adapter = viewPagerAdapter

        val tabs = listOf(
            "Semua Pesanan",
            "Perlu Tagihan",
            "Konfirmasi Pembayaran",
            "Perlu Dikirim",
            "Dikirim",
            "Selesai",
            "Pembatalan",
            "Klaim Pembayaran",
            "Pengiriman Gagal"
        )

        TabLayoutMediator(binding.tabLayoutSells, binding.viewPagerSells) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
