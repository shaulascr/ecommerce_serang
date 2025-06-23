package com.alya.ecommerce_serang.ui.profile.mystore.review

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ReviewViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    private val reviewScore = listOf(
        "all",
        "5",
        "4",
        "3",
        "2",
        "1"
    )

    override fun getItemCount(): Int = reviewScore.size

    override fun createFragment(position: Int): Fragment {
        return ReviewListFragment.newInstance(reviewScore[position])
    }
}