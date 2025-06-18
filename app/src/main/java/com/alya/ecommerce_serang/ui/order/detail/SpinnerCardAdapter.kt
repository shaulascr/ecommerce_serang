package com.alya.ecommerce_serang.ui.order.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.alya.ecommerce_serang.R

class SpinnerCardAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View shown when Spinner is collapsed
        return createCardView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View shown for dropdown items
        return createCardView(position, convertView, parent)
    }

    private fun createCardView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.item_dialog_spinner_card, parent, false)
        val textView = view.findViewById<TextView>(R.id.tvOption)
        textView.text = items[position]
        return view
    }
}
