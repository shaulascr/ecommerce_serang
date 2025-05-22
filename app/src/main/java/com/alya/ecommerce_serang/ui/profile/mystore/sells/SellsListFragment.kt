package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.response.store.orders.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.FragmentSellsListBinding
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel

class SellsListFragment : Fragment() {

    private var _binding: FragmentSellsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var  sessionManager: SessionManager

    private val viewModel: SellsViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val sellsRepository = SellsRepository(apiService)
            SellsViewModel(sellsRepository)
        }
    }

    private lateinit var sellsAdapter: SellsAdapter
    private var status: String = "all"

    companion object {
        private const val TAG = "SellsListFragment"
        private const val ARG_STATUS = "status"

        fun newInstance(status: String): SellsListFragment {
            Log.d(TAG, "=== Creating new instance ===")
            Log.d(TAG, "Status: '$status'")

            return SellsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_STATUS, status)
                }
                Log.d(TAG, "Fragment instance created with status: '$status'")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
        arguments?.let {
            status = it.getString(ARG_STATUS) ?: "all"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSellsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSellsList()
        loadSells()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun setupRecyclerView() {
        sellsAdapter = SellsAdapter(
            onOrderClickListener = { sells ->
                Log.d(TAG, "Order clicked: ${sells.orderId} in status '$status'")
                navigateToSellsDetail(sells)
            },
            viewModel = viewModel
        )

        Log.d(TAG, "Setting adapter fragment status to: '$status'")
        sellsAdapter.setFragmentStatus(status)

        binding.rvSells.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sellsAdapter
            Log.d(TAG, "RecyclerView configured with LinearLayoutManager and adapter")
        }

        // Log RecyclerView visibility and properties
        Log.d(TAG, "RecyclerView visibility: ${binding.rvSells.visibility}")
        Log.d(TAG, "RecyclerView parent: ${binding.rvSells.parent}")
    }

    private fun observeSellsList() {
        viewModel.sells.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ViewState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (result.data.isNullOrEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvSells.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvSells.visibility = View.VISIBLE
                        result.data.forEachIndexed { index, order ->
                            Log.d(TAG, "Order $index:")
                            Log.d(TAG, "  - ID: ${order.orderId}")
                            Log.d(TAG, "  - Status: ${order.status}")
                            Log.d(TAG, "  - Customer: ${order.username}")
                            Log.d(TAG, "  - Total: ${order.totalAmount}")
                            Log.d(TAG, "  - Items: ${order.orderItems?.size ?: 0}")
                        }
                        sellsAdapter.submitList(result.data)

                        Log.d(TAG, "Final UI state:")
                        Log.d(TAG, "  - ProgressBar visibility: ${binding.progressBar.visibility}")
                        Log.d(TAG, "  - EmptyState visibility: ${binding.tvEmptyState.visibility}")
                        Log.d(TAG, "  - RecyclerView visibility: ${binding.rvSells.visibility}")
                        Log.d(TAG, "  - Adapter item count: ${sellsAdapter.itemCount}")
                    }
                }
                is ViewState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is ViewState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvSells.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun loadSells() {
        Log.d(TAG, "Calling viewModel.getSellList('$status')")
        viewModel.getSellList(status)
    }

    private fun navigateToSellsDetail(sells: OrdersItem) {
        // In a real app, you would navigate to sells detail screen
        // For example: findNavController().navigate(SellsListFragmentDirections.actionToSellsDetail(sells.orderId))
        Toast.makeText(requireContext(), "Order ID: ${sells.orderId}", Toast.LENGTH_SHORT).show()
    }
}