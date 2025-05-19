package com.alya.ecommerce_serang.ui.order.history.cancelorder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.CancelOrderReq

class CancelReasonAdapter(
    context: Context,
    private val reasons: List<CancelOrderReq>
) : ArrayAdapter<CancelOrderReq>(context, 0, reasons) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
        val reason = getItem(position) ?: return recycledView ?: View(context)

        val view = recycledView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_cancel_order, parent, false)

        val tvReason = view.findViewById<TextView>(R.id.tv_reason)
        tvReason.text = reason.reason

        return view
    }
}