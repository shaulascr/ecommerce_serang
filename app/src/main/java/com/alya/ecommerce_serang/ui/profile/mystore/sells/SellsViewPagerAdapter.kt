package com.alya.ecommerce_serang.ui.profile.mystore.sells

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SellsViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    // Define all possible sells statuses - keeping your original list
    private val sellsStatuses = listOf(
        "all",          // Position 0: "Semua Pesanan"
        "pending",      // Position 1: "Menunggu Tagihan"
        "unpaid",       // Position 2: "Konfirmasi Bayar"
        "paid",         // Position 3: "Diproses"
        "processed",    // Position 4: "Sudah Dibayar"
        "shipped",      // Position 5: "Dikirim"
        "completed",    // Position 6: "Selesai"
        "canceled"      // Position 7: "Dibatalkan"
    )

    override fun getItemCount(): Int = sellsStatuses.size

    override fun createFragment(position: Int): Fragment {
        // Create a new instance of SellsListFragment with the appropriate status
        return SellsListFragment.newInstance(sellsStatuses[position])
    }
}