package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.response.store.sells.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.FragmentSellsListBinding
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.ui.profile.mystore.MyStoreActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.payment.DetailPaymentActivity
import com.alya.ecommerce_serang.ui.profile.mystore.sells.shipment.DetailShipmentActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch

class SellsListFragment : Fragment() {

    private var _binding: FragmentSellsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

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
            Log.d(TAG, "Creating new instance with status: '$status'")

            return SellsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_STATUS, status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        sessionManager = SessionManager(requireContext())
        arguments?.let {
            status = it.getString(ARG_STATUS) ?: "all"
        }
        Log.d(TAG, "Fragment status set to: '$status'")

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")

        _binding = FragmentSellsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSells()
        setupRecyclerView()
        observeSellsList()
        observePaymentConfirmation()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        sellsAdapter = SellsAdapter(
            onOrderClickListener = { order ->
                Log.d(TAG, "Order clicked: ${order.orderId}")
                navigateToSellsDetail(order)
            },
            viewModel = viewModel
        )

        sellsAdapter.setFragmentStatus(status)
        Log.d(TAG, "Adapter fragment status set to: '$status'")

        binding.rvSells.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sellsAdapter
        }
        Log.d(TAG, "RecyclerView configured")
    }

    private fun observeSellsList() {
        Log.d(TAG, "Setting up sells list observer")

        viewModel.sells.observe(viewLifecycleOwner) { result ->
            Log.d(TAG, "Sells list observer triggered with result: ${result.javaClass.simpleName}")
            when (result) {
                is ViewState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "Data received: ${result.data?.size ?: 0} items")

                    if (result.data.isNullOrEmpty()) {
                        binding.rvSells.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.VISIBLE
                        Log.d(TAG, "Showing empty state")

                    } else {
                        Log.d(TAG, "✅ Data is available: ${result.data.size} items")
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvSells.visibility = View.VISIBLE

                        result.data.take(3).forEachIndexed { index, order ->
                            Log.d(TAG, "Order ${index + 1}: ID=${order.orderId}, Status=${order.status}, Customer=${order.username}")
                        }

                        sellsAdapter.submitList(result.data)
                        Log.d(TAG, "Data submitted to adapter")
                        Log.d(TAG, "Adapter item count: ${sellsAdapter.itemCount}")                    }
                }
                is ViewState.Error -> {
                    Log.e(TAG, "❌ ViewState.Error received: ${result.message}")

                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is ViewState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun observePaymentConfirmation() {
        viewModel.confirmPaymentStore.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Handle loading state if needed
                }
                is Result.Success -> {
                    Toast.makeText(requireContext(), "Payment confirmed successfully!", Toast.LENGTH_SHORT).show()
                    loadSells()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Failed to payment confirm order: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSells() {
        Log.d(TAG, "Loading sells with status: '$status'")
        viewModel.getSellList(status)
        Log.d(TAG, "getSellList called")
    }

    private fun navigateToSellsDetail(order: OrdersItem) {
        Log.d(TAG, "Navigating to sells detail for order: ${order.orderId}")
        val context = requireContext()
        val intent = when (status) {
            "paid" -> Intent(context, DetailPaymentActivity::class.java)
            "processed" -> Intent(context, DetailShipmentActivity::class.java)
            else -> Intent(context, DetailSellsActivity::class.java)
        }
        intent.putExtra("sells_data", Gson().toJson(order))
        context.startActivity(intent)
    }

    private fun getAllOrderCountsAndNavigate() {
        lifecycleScope.launch {
            try {
                // Show loading if needed
                binding.progressBar.visibility = View.VISIBLE

                val allCounts = viewModel.getAllStatusCounts()

                binding.progressBar.visibility = View.GONE

                val intent = Intent(requireContext(), MyStoreActivity::class.java)
                intent.putExtra("total_unpaid", allCounts["unpaid"])
                intent.putExtra("total_paid", allCounts["paid"])
                intent.putExtra("total_processed", allCounts["processed"])
                Log.d("SellsListFragment", "Total orders: unpaid=${allCounts["unpaid"]}, processed=${allCounts["processed"]}, Paid=${allCounts["paid"]}")


            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error getting order counts: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getSellList(status)
        observeSellsList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}