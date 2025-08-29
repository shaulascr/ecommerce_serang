package com.alya.ecommerce_serang.ui.profile.mystore.sells

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SellsViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    // Define all possible sells statuses - keeping your original list
    val sellsStatuses = listOf(
        "all",
        "unpaid",
        "paid",
        "processed",
        "shipped",
        "delivered",
        "completed",
        "canceled"
    )

    override fun getItemCount(): Int = sellsStatuses.size

    override fun createFragment(position: Int): Fragment {
        // Create a new instance of SellsListFragment with the appropriate status
        return SellsListFragment.newInstance(sellsStatuses[position])
    }
//    override fun getItemId(position: Int): Long = sellsStatuses[position].hashCode().toLong()
//    override fun containsItem(itemId: Long): Boolean =
//        sellsStatuses.any { it.hashCode().toLong() == itemId }
}