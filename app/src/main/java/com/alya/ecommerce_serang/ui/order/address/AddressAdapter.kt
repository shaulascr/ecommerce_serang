package com.alya.ecommerce_serang.ui.order.address

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.profile.AddressesItem
import com.google.android.material.card.MaterialCardView

class AddressAdapter(
    private val onAddressClick: (Int) -> Unit
) : ListAdapter<AddressesItem, AddressAdapter.AddressViewHolder>(DIFF_CALLBACK) {

    private var selectedAddressId: Int? = null

    fun setSelectedAddressId(id: Int?) {
        selectedAddressId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = getItem(position)
        holder.bind(address, selectedAddressId == address.id)
        holder.itemView.setOnClickListener {
            onAddressClick(address.id)
        }
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_name_address)
        private val tvDetail: TextView = itemView.findViewById(R.id.tv_detail_address)
        private val card: MaterialCardView = itemView as MaterialCardView

        fun bind(address: AddressesItem, isSelected: Boolean) {
            tvName.text = address.recipient
            tvDetail.text = "${address.street}, ${address.subdistrict}, ${address.phone}"

            card.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isSelected) R.color.blue_50 else R.color.white
                )
            )
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AddressesItem>() {
            override fun areItemsTheSame(oldItem: AddressesItem, newItem: AddressesItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AddressesItem, newItem: AddressesItem) =
                oldItem == newItem
        }
    }
}
