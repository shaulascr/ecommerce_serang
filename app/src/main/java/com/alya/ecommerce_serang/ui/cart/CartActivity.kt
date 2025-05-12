package com.alya.ecommerce_serang.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.ActivityCartBinding
import com.alya.ecommerce_serang.ui.order.CheckoutActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var storeAdapter: StoreAdapter

    private val viewModel: CartViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            CartViewModel(orderRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Apply insets to your root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.getCart()
    }

    private fun setupRecyclerView() {
        storeAdapter = StoreAdapter(
            onStoreCheckChanged = { storeId, isChecked ->
                if (isChecked) {
                    viewModel.toggleStoreSelection(storeId)
                } else {
                    viewModel.toggleStoreSelection(storeId)
                }
            },
            onItemCheckChanged = { cartItemId, storeId, isChecked ->
                viewModel.toggleItemSelection(cartItemId, storeId)
            },
            onItemQuantityChanged = { cartItemId, quantity ->
                viewModel.updateCartItem(cartItemId, quantity)
            },
            onItemDeleted = { cartItemId ->
                viewModel.deleteCartItem(cartItemId)
            }
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = storeAdapter
        }
    }

    private fun setupListeners() {
        binding.cbSelectAll.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleSelectAll()
        }

        binding.btnCheckout.setOnClickListener {
            if (viewModel.totalSelectedCount.value ?: 0 > 0) {
                val selectedItems = viewModel.prepareCheckout()
                if (selectedItems.isNotEmpty()) {
                    // Check if all items are from the same store
                    val storeId = viewModel.activeStoreId.value
                    if (storeId != null) {
                        // Get cart item ids to pass to checkout
                        val cartItemIds = selectedItems.map { it.cartItemId }
                        CheckoutActivity.startForCart(this, cartItemIds)
                    } else {
                        Toast.makeText(this, "Please select items from a single store only", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Pilih produk terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShopNow.setOnClickListener {
            // Navigate to product listing/home
            //implement home or search activity
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.cartItems.observe(this) { cartItems ->
            if (cartItems.isNullOrEmpty()) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                storeAdapter.submitList(cartItems)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator if needed
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.totalPrice.observe(this) { totalPrice ->
            binding.tvTotalPrice.text = formatCurrency(totalPrice)
        }

        viewModel.totalSelectedCount.observe(this) { count ->
            binding.btnCheckout.text = "Beli ($count)"
        }

        viewModel.selectedItems.observe(this) { selectedItems ->
            viewModel.selectedStores.value?.let { selectedStores ->
                viewModel.activeStoreId.value?.let { activeStoreId ->
                    storeAdapter.updateSelectedItems(selectedItems, selectedStores, activeStoreId)
                }
            }
        }

        viewModel.allSelected.observe(this) { allSelected ->
            // Update the "select all" checkbox without triggering the listener
            val selectCbAll = binding.cbSelectAll
            selectCbAll.setOnCheckedChangeListener(null)
            selectCbAll.isChecked = allSelected
            selectCbAll.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleSelectAll()
            }
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvCart.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.bottomCheckoutLayout).visibility = View.GONE
        } else {
            binding.rvCart.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.bottomCheckoutLayout).visibility = View.VISIBLE
        }
    }

    private fun formatCurrency(amount: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }


}

