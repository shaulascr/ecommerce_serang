package com.alya.ecommerce_serang.ui.profile.mystore.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.databinding.FragmentChangeStockBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChangeStockBottomSheetFragment(
    private val product: ProductsItem,
    private val onSave: (productId: Int, newStock: Int) -> Unit
): BottomSheetDialogFragment() {
    private var _binding: FragmentChangeStockBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var stock = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChangeStockBottomSheetBinding.inflate(inflater, container, false)

        binding.header.headerTitle.text = "Atur Stok"
        binding.header.headerLeftIcon.setImageResource(R.drawable.ic_close)
        binding.header.headerLeftIcon.setOnClickListener { dismiss() }

        stock = product.stock
        updateStock()

        binding.btnMinus.setOnClickListener {
            if (stock > 0) stock--
            updateStock()
        }

        binding.btnPlus.setOnClickListener {
            stock++
            updateStock()
        }

        binding.btnSave.setOnClickListener {
            onSave(product.id, stock)
            dismiss()
        }

        return binding.root
    }

    private fun updateStock() {
        binding.edtStock.setText(stock.toString())
        if (stock == 0) {
            binding.btnMinus.isEnabled = false
            binding.btnMinus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black_100))
        } else {
            binding.btnMinus.isEnabled = true
            binding.btnMinus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue_500))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}