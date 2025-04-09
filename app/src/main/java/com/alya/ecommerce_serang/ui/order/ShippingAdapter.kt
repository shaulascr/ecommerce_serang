package com.alya.ecommerce_serang.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.order.ServicesItem

class ShippingAdapter(
    private val onItemSelected: (ServicesItem) -> Unit
) : RecyclerView.Adapter<ShippingAdapter.ShippingViewHolder>() {

    private var services = listOf<ServicesItem>()
    private var selectedPosition = -1

    fun submitList(newList: List<ServicesItem>) {
        services = newList
        notifyDataSetChanged()
    }

    inner class ShippingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val courierName = itemView.findViewById<TextView>(R.id.courier_name_cost)
        private val estDate = itemView.findViewById<TextView>(R.id.est_date)
        private val costPrice = itemView.findViewById<TextView>(R.id.cost_price)
        private val radioButton = itemView.findViewById<RadioButton>(R.id.radio_btn_cost)

        fun bind(service: ServicesItem, isSelected: Boolean) {
            courierName.text = service.service // already includes courier name from ViewModel
            estDate.text = "Estimasi ${service.etd}"
            costPrice.text = "Rp${service.cost}"
            radioButton.isChecked = isSelected

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    selectedPosition = adapterPosition
                    notifyDataSetChanged()
                    onItemSelected(service)
                }
            }

            radioButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    selectedPosition = adapterPosition
                    notifyDataSetChanged()
                    onItemSelected(service)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShippingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shipping_order, parent, false)
        return ShippingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShippingViewHolder, position: Int) {
        val service = services[position]
        holder.bind(service, position == selectedPosition)
    }

    override fun getItemCount(): Int = services.size
}
