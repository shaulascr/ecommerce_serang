package com.alya.ecommerce_serang.ui.profile.mystore.balance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.topup.TopUp
import com.alya.ecommerce_serang.ui.profile.mystore.balance.BalanceTransactionAdapter.BalanceTransactionViewHolder

class BalanceTransactionAdapter : ListAdapter<TopUp, BalanceTransactionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceTransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_balance_transaction, parent, false)
        return BalanceTransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: BalanceTransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BalanceTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_balance_trans_date)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_balance_trans_title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_balance_trans_desc)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_balance_trans_amount)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_balance_trans_icon)
        private val divider: View = itemView.findViewById(R.id.divider_balance_trans)

        fun bind(topUp: TopUp) {
            // Set date
            tvDate.text = topUp.getFormattedDate()

            // Set title
            tvTitle.text = "Isi Ulang Saldo"

            // Set description - payment details
            val paymentMethod = topUp.paymentMethod
            val accountName = topUp.accountName ?: ""
            val desc = if (accountName.isNotEmpty()) {
                "Isi ulang dari $paymentMethod $accountName"
            } else {
                "Isi ulang dari $paymentMethod"
            }
            tvDesc.text = desc

            // Set amount
            tvAmount.text = topUp.getFormattedAmount()

            // Set color based on status
            val context = itemView.context
            val activeColor = ContextCompat.getColor(context, R.color.blue_500)
            val pendingColor = ContextCompat.getColor(context, R.color.black_500)

            when (topUp.status.lowercase()) {
                "approved" -> {
                    tvAmount.setTextColor(activeColor)
                    ivIcon.setImageResource(R.drawable.ic_graph_arrow_increase)
                }
                "pending" -> {
                    tvAmount.setTextColor(pendingColor)
                }
                else -> {
                    tvAmount.setTextColor(activeColor)
                }
            }

            // Show divider for all items except the last one
            divider.visibility = if (bindingAdapterPosition == itemCount - 1) View.GONE else View.VISIBLE
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TopUp>() {
            override fun areItemsTheSame(oldItem: TopUp, newItem: TopUp): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TopUp, newItem: TopUp): Boolean {
                return oldItem == newItem
            }
        }
    }
}