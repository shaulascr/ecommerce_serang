package com.alya.ecommerce_serang.ui.profile.mystore.profile.payment_info

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.PaymentInfo
import com.bumptech.glide.Glide

class PaymentInfoAdapter(
    private val onDeleteClick: (PaymentInfo) -> Unit
) : ListAdapter<PaymentInfo, PaymentInfoAdapter.PaymentInfoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentInfoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_info, parent, false)
        return PaymentInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBankName: TextView = itemView.findViewById(R.id.tv_bank_name)
        private val tvAccountName: TextView = itemView.findViewById(R.id.tv_account_name)
        private val tvBankNumber: TextView = itemView.findViewById(R.id.tv_bank_number)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
        private val layoutQris: LinearLayout = itemView.findViewById(R.id.layout_qris)
        private val ivQris: ImageView = itemView.findViewById(R.id.iv_qris)

        fun bind(paymentInfo: PaymentInfo) {
            tvBankName.text = paymentInfo.bankName
            tvAccountName.text = paymentInfo.accountName ?: ""
            tvBankNumber.text = paymentInfo.bankNum

            // Handle QRIS image if available
            if (paymentInfo.qrisImage != null && paymentInfo.qrisImage.isNotEmpty() && paymentInfo.qrisImage != "null") {
                layoutQris.visibility = View.VISIBLE
                // Make sure the URL is correct by handling both relative and absolute paths
                val imageUrl = if (paymentInfo.qrisImage.startsWith("http")) {
                    paymentInfo.qrisImage
                } else {
                    "http://192.168.100.156:3000${paymentInfo.qrisImage}"
                }

                Log.d("PaymentMethodAdapter", "Loading QRIS image from: $imageUrl")

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivQris)
            } else {
                layoutQris.visibility = View.GONE
            }

            ivDelete.setOnClickListener {
                onDeleteClick(paymentInfo)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PaymentInfo>() {
            override fun areItemsTheSame(oldItem: PaymentInfo, newItem: PaymentInfo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PaymentInfo, newItem: PaymentInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}