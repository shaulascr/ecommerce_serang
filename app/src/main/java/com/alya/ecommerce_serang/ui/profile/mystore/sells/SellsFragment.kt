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
    companion object {
        private const val ARG_INITIAL_STATUS = "arg_initial_status"

        fun newInstance(initialStatus: String?): SellsFragment =
            SellsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_STATUS, initialStatus)
                }
            }
    }

    private var _binding: FragmentSellsBinding? = null
//    private var currentSearchQuery = ""
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
        jumpToInitialStatusIfAny()
//        binding.viewPagerSells.post { currentPage()?.filter(currentSearchQuery) }
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

    private fun jumpToInitialStatusIfAny() {
        val initial = arguments?.getString(ARG_INITIAL_STATUS)?.trim().orEmpty()
        if (initial.isEmpty()) return

        // Try adapter’s list first
        var index = viewPagerAdapter.sellsStatuses.indexOf(initial)

        // Fallback mapping (keeps working if the adapter changes order names)
        if (index < 0) {
            index = when (initial) {
                "all" -> 0
                "unpaid" -> 1
                "paid" -> 2
                "processed" -> 3
                "shipped" -> 4
                "delivered" -> 5
                "completed" -> 6
                "canceled" -> 7
                else -> 0
            }
        }

        if (index in 0 until (binding.viewPagerSells.adapter?.itemCount ?: 0)) {
            // Ensure pager is ready, then jump without animation
            binding.viewPagerSells.post {
                binding.viewPagerSells.setCurrentItem(index, false)
                // Make sure ViewModel filter matches the shown tab
                sellsVm.updateStatus(
                    viewPagerAdapter.sellsStatuses.getOrNull(index) ?: initial,
                    forceRefresh = true
                )
            }
        }
    }

//    fun onSearchQueryChanged(q: String) {
//        currentSearchQuery = q
//        currentPage()?.filter(q)
//    }

//    private fun currentPage(): SellsListFragment? {
//        val pos = binding.viewPagerSells.currentItem
//        val tag = "f${viewPagerAdapter.getItemId(pos)}"  // requires stable ids in adapter (see step 4)
//        return childFragmentManager.findFragmentByTag(tag) as? SellsListFragment
//    }

    private fun statusPage() {
        binding.viewPagerSells.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val status = viewPagerAdapter.sellsStatuses[position]
                    sellsVm.updateStatus(status, forceRefresh = true)
                    // re-apply query when user switches tab
//                    currentPage()?.filter(currentSearchQuery)
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