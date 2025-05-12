package com.alya.ecommerce_serang.ui.order

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.customer.product.PaymentInfoItem
import com.alya.ecommerce_serang.databinding.ItemPaymentMethodBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class PaymentMethodAdapter(
    private val paymentMethods: List<PaymentInfoItem>,
    private val onPaymentSelected: (PaymentInfoItem) -> Unit
) : RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>() {

    // Track the selected payment by ID
    private var selectedPaymentId: Int? = null

    class PaymentMethodViewHolder(val binding: ItemPaymentMethodBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val binding = ItemPaymentMethodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaymentMethodViewHolder(binding)
    }

    override fun getItemCount(): Int = paymentMethods.size

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        val payment = paymentMethods[position]

        with(holder.binding) {
            // Set payment method name
            tvPaymentMethodName.text = payment.name

            // Set radio button state based on selected payment ID
            rbPaymentMethod.isChecked = payment.id == selectedPaymentId

            // Debug log
            Log.d("PaymentAdapter", "Binding item ${payment.name}, checked=${rbPaymentMethod.isChecked}")

            // Load payment icon if available
            if (!payment.qrisImage.isNullOrEmpty()) {
                Glide.with(ivPaymentMethod.context)
                    .load(payment.qrisImage)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.outline_store_24)
                            .error(R.drawable.outline_store_24)
                    )
                    .into(ivPaymentMethod)
            } else {
                // Default icon for bank transfers
                ivPaymentMethod.setImageResource(R.drawable.outline_store_24)
            }

            // IMPORTANT: We need to fix the click handling to prevent re-fetching
            val clickListener = View.OnClickListener {
                val previousSelectedId = selectedPaymentId
                selectedPaymentId = payment.id

                // Force the radio button to be checked
                rbPaymentMethod.isChecked = true

                // Only notify if there was a change in selection
                if (previousSelectedId != payment.id) {
                    notifyItemChanged(position)

                    // Notify previous selection if it exists
                    if (previousSelectedId != null) {
                        val prevPosition = paymentMethods.indexOfFirst { it.id == previousSelectedId }
                        if (prevPosition >= 0) {
                            notifyItemChanged(prevPosition)
                        }
                    }

                    // Call the callback ONLY ONCE
                    onPaymentSelected(payment)

                    Log.d("PaymentAdapter", "Payment selected: ${payment.name}")
                }
            }

            // Apply the same click listener to both the root and the radio button
            root.setOnClickListener(clickListener)
            rbPaymentMethod.setOnClickListener(clickListener)
        }
    }

    // Set selected payment
    fun setSelectedPaymentId(paymentId: Int) {
        if (selectedPaymentId != paymentId) {
            val previousSelectedId = selectedPaymentId
            selectedPaymentId = paymentId

            Log.d("PaymentAdapter", "Setting selected payment ID to: $paymentId")

            // Update affected items only
            if (previousSelectedId != null) {
                val prevPosition = paymentMethods.indexOfFirst { it.id == previousSelectedId }
                if (prevPosition >= 0) {
                    notifyItemChanged(prevPosition)
                }
            }

            val newPosition = paymentMethods.indexOfFirst { it.id == paymentId }
            if (newPosition >= 0) {
                notifyItemChanged(newPosition)
            }
        }
    }

    // Set selected payment object
    fun setSelectedPayment(payment: PaymentInfoItem) {
        setSelectedPaymentId(payment.id)
    }
}