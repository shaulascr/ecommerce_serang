package com.alya.ecommerce_serang.ui.order.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.FragmentOrderHistoryBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.tabs.TabLayoutMediator

class OrderHistoryFragment : Fragment() {

    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private val historyVm: HistoryViewModel by activityViewModels {
        BaseViewModelFactory {
            val api = ApiConfig.getApiService(SessionManager(requireContext()))
            HistoryViewModel(OrderRepository(api))
        }
    }

    private lateinit var viewPagerAdapter: OrderViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupViewPager()


    }

    private fun setupViewPager() {
        // Initialize the ViewPager adapter
        viewPagerAdapter = OrderViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = viewPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.all_orders)
                1 -> getString(R.string.unpaid_orders)
                2 -> getString(R.string.paid_orders)
                3 -> getString(R.string.processed_orders)
                4 -> getString(R.string.shipped_orders)
                5 -> getString(R.string.completed_orders)
                6 -> getString(R.string.canceled_orders)
                else -> "Tab $position"
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val status = viewPagerAdapter.orderStatuses[position]
                    /* setStatus() is the API we added earlier; TRUE → always re‑query */
                    historyVm.updateStatus(status, forceRefresh = true)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}