package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.ktor.client.utils.EmptyContent.status

class SellsViewPagerAdapter(fragmentActivity: FragmentActivity)
    : FragmentStateAdapter(fragmentActivity) {

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

    init {
        Log.d(TAG, "=== ViewPager Status Mapping ===")
        sellsStatuses.forEachIndexed { index, status ->
            val tabText = when(index) {
                0 -> "Semua Pesanan"
                1 -> "Menunggu Tagihan"
                2 -> "Konfirmasi Bayar"
                3 -> "Diproses"
                4 -> "Sudah Dibayar"
                5 -> "Dikirim"
                6 -> "Selesai"
                7 -> "Dibatalkan"
                else -> "Tab $index"
            }
            Log.d(TAG, "Position $index: '$tabText' → Status: '$status'")
        }
        Log.d(TAG, "=== End Mapping ===")
    }


    override fun getItemCount(): Int = sellsStatuses.size

    override fun createFragment(position: Int): Fragment {
        Log.d(TAG, "Creating fragment for position $position with status: '$status'")
        return SellsListFragment.newInstance(sellsStatuses[position])
    }

    companion object {
        private const val TAG = "SellsViewPagerAdapter"
    }
}