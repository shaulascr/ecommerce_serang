package com.alya.ecommerce_serang.ui.order.history.cancelorder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CancelOrderReq
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.ui.order.history.HistoryViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CancelOrderBottomSheet(
    private val orderId: Int,
    private val onOrderCancelled: () -> Unit
) : BottomSheetDialogFragment() {
    private lateinit var sessionManager: SessionManager

    private val viewModel: HistoryViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            HistoryViewModel(orderRepository)
        }
    }
    private var selectedReason: CancelOrderReq? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_cancel_order_bottom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val spinnerReason = view.findViewById<Spinner>(R.id.spinner_reason)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm)

        // Set the title
        tvTitle.text = "Batalkan Pesanan #$orderId"

        // Set up the spinner with cancellation reasons
        setupReasonSpinner(spinnerReason)

        // Handle button clicks
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnConfirm.setOnClickListener {
            if (selectedReason == null) {
                Toast.makeText(context, "Please select a reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cancelOrder()
        }
    }

    private fun setupReasonSpinner(spinner: Spinner) {
        val reasons = getCancellationReasons()
        val adapter = CancelReasonAdapter(requireContext(), reasons)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedReason = reasons[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedReason = null
            }
        }
    }

    private fun getCancellationReasons(): List<CancelOrderReq> {
        // These should ideally come from the server or a configuration
        return listOf(
            CancelOrderReq(1, "Berubah pikiran"),
            CancelOrderReq(2, "Menemukan pilihan yang lebih baik"),
            CancelOrderReq(3, "Kesalahan pemesanan"),
            CancelOrderReq(4, "Waktu pengiriman lama"),
            CancelOrderReq(5, "Lainnya")
        )
    }

    private fun cancelOrder() {
        // Validate reason selection
        if (selectedReason == null) {
            Toast.makeText(context, "Mohon pilih alasan pembatalan", Toast.LENGTH_SHORT).show()
            return
        }

        // Create cancel request
        val cancelRequest = CancelOrderReq(
            orderId = orderId,
            reason = selectedReason!!.reason
        )
        Log.d(TAG, "Sending cancel request to ViewModel: orderId=${cancelRequest.orderId}, reason='${cancelRequest.reason}'")


        // Submit the cancellation
        viewModel.cancelOrder(cancelRequest)

        // Observe the status
        viewModel.cancelOrderStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show loading indicator
//                    showLoading(true)
                }
                is Result.Success -> {
                    // Hide loading indicator
                    showLoading(false)

                    // Show success message
                    Toast.makeText(
                        context,
                        "Pesanan berhasil dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "Cancel order status: SUCCESS, message: ${result.data.message}")

                    // Notify callback and close dialog
                    onOrderCancelled()
                    dismiss()
                }
                is Result.Error -> {
                    // Hide loading indicator
                    showLoading(false)
                    Log.e(TAG, "Cancel order status: ERROR", result.exception)


                    // Show error message
                    val errorMsg = result.exception.message ?: "Gagal membatalkan pesanan"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

//    private fun showLoading(isLoading: Boolean) {
//        binding.progressBar.isVisible = isLoading
//        binding.btnCancel.isEnabled = !isLoading
//        binding.btnConfirm.isEnabled = !isLoading
//    }

    private fun showLoading(isLoading: Boolean) {
        // Implement loading indicator if needed
    }

    companion object {
        const val TAG = "CancelOrderBottomSheet"
    }
}