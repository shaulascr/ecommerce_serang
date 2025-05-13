package com.alya.ecommerce_serang.ui.profile.mystore.sells

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SellsViewPagerAdapter(fragmentActivity: FragmentActivity)
    : FragmentStateAdapter(fragmentActivity) {

    private val sellsStatuses = listOf(
        "all",          // Semua Pesanan
        "pending",      // Perlu Tagihan
        "processed",    // Konfirmasi Pembayaran
        "paid",         // Perlu Dikirim
        "shipped",      // Dikirim
        "delivered",    // Dikirim
        "completed",    // Selesai
        "canceled",     // Dibatalkan
        TODO("Klaim Pembayaran dan Pengajuan Komplain belum ada statusnya")
    )

    override fun getItemCount(): Int = sellsStatuses.size

    override fun createFragment(position: Int): Fragment {
        return SellsListFragment.newInstance(sellsStatuses[position])
    }
}