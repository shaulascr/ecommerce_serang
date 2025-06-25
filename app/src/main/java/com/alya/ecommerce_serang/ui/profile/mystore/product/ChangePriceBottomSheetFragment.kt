package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.databinding.FragmentChangePriceBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChangePriceBottomSheetFragment(
    private val product: ProductsItem,
    private val onSave: (productId: Int, newPrice: Int) -> Unit
) : BottomSheetDialogFragment() {
    private var _binding: FragmentChangePriceBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePriceBottomSheetBinding.inflate(inflater, container, false)

        binding.header.headerTitle.text = "Atur Harga"
        binding.header.headerLeftIcon.setImageResource(R.drawable.ic_close)
        binding.header.headerLeftIcon.setOnClickListener { dismiss() }

        binding.edtPrice.setText(product.price)

        binding.btnSave.setOnClickListener {
            val newPrice = binding.edtPrice.text.toString().replace(".", "").toIntOrNull()
            if (newPrice != null && newPrice > 0) {
                onSave(product.id, newPrice)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Masukkan harga yang valid", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}