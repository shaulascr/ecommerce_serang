package com.alya.ecommerce_serang.utils

import android.os.Build
import android.text.InputFilter
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

fun Fragment.setLightStatusBar(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requireActivity().window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    } else {
        @Suppress("DEPRECATION")
        requireActivity().window.decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

    }
}

public fun applyLiveCounter(
    editText: EditText,
    tvCount: TextView,
    tvMax: TextView
) {
    val max = tvMax.text.toString().toIntOrNull() ?: Int.MAX_VALUE

    // Replace any existing LengthFilter with the new max
    val current = editText.filters?.toMutableList() ?: mutableListOf()
    current.removeAll { it is InputFilter.LengthFilter }
    current.add(InputFilter.LengthFilter(max))
    editText.filters = current.toTypedArray()

    // Set initial count (handles prefilled text / edit mode)
    tvCount.text = (editText.text?.length ?: 0).toString()

    // Update on change
    editText.doAfterTextChanged {
        val len = it?.length ?: 0
        tvCount.text = len.toString()
    }
}