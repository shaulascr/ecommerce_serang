package com.alya.ecommerce_serang.ui.profile.mystore.sells

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SellsViewPagerAdapter(fragmentActivity: FragmentActivity)
    : FragmentStateAdapter(fragmentActivity) {

    private val sellsStatuses = listOf(
        "all",          // Semua Pesanan
        "pending",       // Pesanan Masuk
        "processed",    // Konfirmasi Pembayaran
        "paid",         // Perlu Dikirim
        "shipped",      // Dikirim
        "completed",    // Selesai
        "canceled",     // Dibatalkan
        "payment_onhold"
    )

    override fun getItemCount(): Int = sellsStatuses.size

    override fun createFragment(position: Int): Fragment {
        return SellsListFragment.newInstance(sellsStatuses[position])
    }
}