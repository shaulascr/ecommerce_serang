package com.alya.ecommerce_serang.ui.profile.mystore.sells.main

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.alya.ecommerce_serang.R

class SellsFragment : Fragment() {

    companion object {
        fun newInstance() = SellsFragment()
    }

    private val viewModel: SellsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_sells, container, false)
    }

}