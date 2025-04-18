package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.databinding.FragmentProductOptionsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProductOptionsBottomSheetFragment(private val product: ProductsItem) : BottomSheetDialogFragment() {

    private var _binding: FragmentProductOptionsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductOptionsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditProduct.setOnClickListener {
            // Handle editing product
            // Example: Open the edit activity or fragment
            dismiss()
        }

        binding.btnDeleteProduct.setOnClickListener {
            // Handle deleting product
            // Example: Show confirmation dialog
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}