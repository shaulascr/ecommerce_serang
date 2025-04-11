package com.alya.ecommerce_serang.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.product.PaymentItem
import com.alya.ecommerce_serang.databinding.ItemPaymentMethodBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class PaymentMethodAdapter(
    private val paymentMethods: List<PaymentItem>,
    private val onPaymentSelected: (PaymentItem) -> Unit
) : RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>() {

    // Track the selected position
    private var selectedPosition = -1

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
            tvPaymentMethodName.text = payment.bankName

            // Set radio button state
            rbPaymentMethod.isChecked = selectedPosition == position

            // Load payment icon if available
            if (payment.qrisImage.isNotEmpty()) {
                Glide.with(ivPaymentMethod.context)
                    .load(payment.qrisImage)
                    .apply(
                        RequestOptions()
                        .placeholder(R.drawable.outline_store_24)
                        .error(R.drawable.outline_store_24))
                    .into(ivPaymentMethod)
            } else {
                // Default icon for bank transfers
                ivPaymentMethod.setImageResource(R.drawable.outline_store_24)
            }

            // Handle click on the entire item
            root.setOnClickListener {
                selectPayment(position)
                onPaymentSelected(payment)
            }

            // Handle click on the radio button
            rbPaymentMethod.setOnClickListener {
                selectPayment(position)
                onPaymentSelected(payment)
            }
        }
    }

    // Helper method to handle payment selection
    private fun selectPayment(position: Int) {
        if (selectedPosition != position) {
            val previousPosition = selectedPosition
            selectedPosition = position

            // Update UI for previous and new selection
            notifyItemChanged(previousPosition)
            notifyItemChanged(position)
        }
    }

    // Select a payment method programmatically
    fun setSelectedPaymentId(paymentId: Int) {
        val position = paymentMethods.indexOfFirst { it.id == paymentId }
        if (position != -1 && position != selectedPosition) {
            selectPayment(position)
        }
    }
}