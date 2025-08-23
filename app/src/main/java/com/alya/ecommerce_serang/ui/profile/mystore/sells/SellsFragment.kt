package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.FragmentSellsBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.android.material.tabs.TabLayoutMediator

class SellsFragment : Fragment() {

    private var _binding: FragmentSellsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private lateinit var viewPagerAdapter: SellsViewPagerAdapter

    private val sellsVm: SellsViewModel by activityViewModels {
        BaseViewModelFactory {
            val api = ApiConfig.getApiService(SessionManager(requireContext()))
            SellsViewModel(SellsRepository(api))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupViewPager()
    }

    private fun setupViewPager() {
        // Initialize the ViewPager adapter
        viewPagerAdapter = SellsViewPagerAdapter(requireActivity())
        binding.viewPagerSells.adapter = viewPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayoutSells, binding.viewPagerSells) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.all_sells)
                1 -> getString(R.string.unpaid_sells)
                2 -> getString(R.string.paid_sells)
                3 -> getString(R.string.processed_sells)
                4 -> getString(R.string.shipped_sells)
                5 -> getString(R.string.delivered_sells)
                6 -> getString(R.string.completed_sells)
                7 -> getString(R.string.canceled_sells)
                else -> "Tab $position"
            }
        }.attach()

        statusPage()
    }

    private fun statusPage(){
        binding.viewPagerSells.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val status = viewPagerAdapter.sellsStatuses[position]
                    sellsVm.updateStatus(status, forceRefresh = true)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        statusPage()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}