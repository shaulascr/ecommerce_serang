package com.alya.ecommerce_serang.ui.profile.mystore.review

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alya.ecommerce_serang.databinding.FragmentReviewBinding
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.tabs.TabLayoutMediator

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private lateinit var viewPagerAdapter: ReviewViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupViewPager()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ReviewViewPagerAdapter(requireActivity())
        binding.viewPagerReview.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayoutReview, binding.viewPagerReview) { tab, position ->
            tab.text = when (position) {
                0 -> "Semua"
                1 -> "5 Bintang"
                2 -> "4 Bintang"
                3 -> "3 Bintang"
                4 -> "2 Bintang"
                5 -> "1 Bintang"
                else -> "Tab $position"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}