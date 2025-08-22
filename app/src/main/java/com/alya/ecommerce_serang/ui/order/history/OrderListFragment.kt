package com.alya.ecommerce_serang.ui.order.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.dto.OrdersItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.FragmentOrderListBinding
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.ui.order.history.detailorder.DetailOrderStatusActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class OrderListFragment : Fragment(), OrderHistoryAdapter.OrderActionCallbacks {

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private val viewModel: HistoryViewModel by activityViewModels {
        BaseViewModelFactory {
            val api = ApiConfig.getApiService(SessionManager(requireContext()))
            HistoryViewModel(OrderRepository(api))
        }
    }

    private lateinit var orderAdapter: OrderHistoryAdapter

    private var status: String = "all"

    private val detailOrderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            /* force‑refresh the current tab */
            viewModel.updateStatus(status, forceRefresh = true)
        }
    }

    companion object {
        private const val ARG_STATUS = "status"

        fun newInstance(status: String): OrderListFragment {
            return OrderListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_STATUS, status)
                }
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeOrderList()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderHistoryAdapter(
            onOrderClickListener = { order ->
                navigateToOrderDetail(order)
            },
            viewModel = viewModel,
            callbacks = this // Pass this fragment as callback
        )

        orderAdapter.setFragmentStatus(status)

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun observeOrderList() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.orders.collect { state ->
                when (state) {
                    is ViewState.Loading -> {
                        binding.progressBar.isVisible = true
                    }
                    is ViewState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.tvEmptyState.isVisible = true
                        binding.rvOrders.isVisible = false
                        Log.e("OrderListFragment", "Error in order list: ${state.message}")
                    }
                    is ViewState.Success -> {
                        binding.progressBar.isVisible = false
                        val list = state.data
                            .filter { status == "all" || it.displayStatus == status }
                        binding.tvEmptyState.isVisible = list.isEmpty()
                        binding.rvOrders.isVisible    = list.isNotEmpty()
                        orderAdapter.submitList(list)
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.orderCompletionStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(requireContext(),
                        "Pesanan Selesai", Toast.LENGTH_SHORT).show()
                    Log.d("OrderListFragment", "Order selesai")
                    viewModel.updateStatus(status, forceRefresh = true)
                }
                is Result.Error ->
//                    Toast.makeText(requireContext(),
//                        "Failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("OrderListFragment", "Failed: ${result.exception.message}")

                else -> { /* Loading → no UI change */ }
            }
        }

        viewModel.cancelOrderStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(requireContext(),
                        "Pesanan Dibatalkan", Toast.LENGTH_SHORT).show()
                    Log.d("OrderListFragment", "Order dibatalkan")
                    viewModel.updateStatus(status, forceRefresh = true)
                }
                is Result.Error ->
                    Log.e("OrderListFragment", "Failed: ${result.exception.message}")
//                    Toast.makeText(requireContext(),
//                        "Failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                else -> { /* Loading */ }
            }
        }
    }

    private fun navigateToOrderDetail(order: OrdersItem) {
        val intent = Intent(requireContext(), DetailOrderStatusActivity::class.java).apply {
            putExtra("ORDER_ID", order.orderId)
            val actualStatus = if (status == "all") order.displayStatus ?: "" else status
            putExtra("ORDER_STATUS", actualStatus)        }
        detailOrderLauncher.launch(intent)
    }

    override fun onOrderCancelled(orderId: String, success: Boolean, message: String) {
        if (success) {
            Toast.makeText(requireContext(), "Berhasil batalkan pesanan", Toast.LENGTH_SHORT).show()
            Log.d("OrderListFragment", "Order cancel success: $message")
//            loadOrders() // Refresh the list
            if (success) viewModel.updateStatus(status, forceRefresh = true)

        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOrderCompleted(orderId: Int, success: Boolean, message: String) {
        if (success) {
            Toast.makeText(requireContext(), "Pesanan selesai", Toast.LENGTH_SHORT).show()
            Log.d("OrderListFragment", "Pesanan selesai: $message")
//            loadOrders() // Refresh the list
            if (success) viewModel.updateStatus(status, forceRefresh = true)
        } else {
            Log.e("OrderListFragment", "Error Order Complete: $message")
            Toast.makeText(requireContext(), "Terdapat kendala di pesanan selesai", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onShowLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        observeOrderList()
    }
}