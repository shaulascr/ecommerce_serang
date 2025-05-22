package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSellsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupViewPager()
    }

    private fun setupViewPager() {
        viewPagerAdapter = SellsViewPagerAdapter(requireActivity())
        binding.viewPagerSells.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayoutSells, binding.viewPagerSells) {tab, position ->
            tab.text = when(position){
                0 -> getString(R.string.all_orders)
                1 -> getString(R.string.pending_orders)
                2 -> getString(R.string.unpaid_orders)
                3 -> getString(R.string.processed_orders)
                4 -> getString(R.string.paid_orders)
                5 -> getString(R.string.shipped_orders)
                6 -> getString(R.string.completed_orders)
                7 -> getString(R.string.canceled_orders)
                else -> "Tab $position"
            }
        }.attach()

        binding.viewPagerSells.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val statusList = listOf("all", "pending", "unpaid", "processed", "paid", "shipped", "completed", "canceled")
                val selectedStatus = statusList.getOrNull(position) ?: "unknown"
                val tabText = when(position) {
                    0 -> getString(R.string.all_orders)
                    1 -> getString(R.string.pending_orders)
                    2 -> getString(R.string.unpaid_orders)
                    3 -> getString(R.string.processed_orders)
                    4 -> getString(R.string.paid_orders)
                    5 -> getString(R.string.shipped_orders)
                    6 -> getString(R.string.completed_orders)
                    7 -> getString(R.string.canceled_orders)
                    else -> "Tab $position"
                }

                Log.d(TAG, "🔄 *** TAB SWITCHED ***")
                Log.d(TAG, "Selected position: $position")
                Log.d(TAG, "Tab text: '$tabText'")
                Log.d(TAG, "Status for this tab: '$selectedStatus'")
                Log.d(TAG, "*** This should trigger SellsListFragment for '$selectedStatus' ***")
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SellsListFragment"

    }
}
