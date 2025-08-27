package com.alya.ecommerce_serang.ui.cart

import android.os.Bundle
import android.util.Log
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
import com.alya.ecommerce_serang.data.api.response.customer.product.CartItemCheckoutInfo
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

    private var TAG = "Cart Activity"

    private val viewModel: CartViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            CartViewModel(orderRepository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!sessionManager.isLoggedIn()){
            binding.emptyCart.text = "Silahkan masuk terlebih dahulu"
        }

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

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.getCart()
    }

    private fun setupToolbar(){
        binding.header.headerLeftIcon.setOnClickListener{
            finish()
        }
        binding.header.headerTitle.text = "Keranjang"
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
                if (viewModel.hasConsistentWholesaleStatus.value == true) {
                    val selectedItems = viewModel.prepareCheckout()
                    if (selectedItems.isNotEmpty()) {
                        // Check if all items are from the same store
                        val storeId = viewModel.activeStoreId.value
                        if (storeId != null) {
                            // Start checkout with the prepared items
                            startCheckoutWithWholesaleInfo(selectedItems)
                        } else {
                            Toast.makeText(this, "Pilih produk yang sama dengan toko", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Tidak dapat checkout produk grosir dan retail sekaligus", Toast.LENGTH_LONG).show()
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

    private fun startCheckoutWithWholesaleInfo(checkoutItems: List<CartItemCheckoutInfo>) {
        val wholesalePriceMap = viewModel.cartItemWholesalePrice.value ?: emptyMap()

        val updatedItems = checkoutItems.map { info ->
            val wholesalePrice = wholesalePriceMap[info.cartItem.cartItemId]

            val updatedCartItem = if (info.isWholesale && wholesalePrice != null) {
                // Replace the price with wholesale price
                info.cartItem.copy(price = wholesalePrice.toInt())
            } else {
                info.cartItem
            }

            // Debug log
            Log.d(
                TAG,
                "cartItemId: ${updatedCartItem.cartItemId}, " +
                        "isWholesale: ${info.isWholesale}, " +
                        "wholesalePrice: $wholesalePrice, " +
                        "finalPrice: ${updatedCartItem.price}"
            )

            info.copy(cartItem = updatedCartItem)
        }

        val cartItemIds = updatedItems.map { it.cartItem.cartItemId }
        val wholesaleArray = updatedItems.map { it.isWholesale }.toBooleanArray()

        // FIX: Pass wholesale prices as IntArray
        val wholesalePricesArray = updatedItems.map { info ->
            if (info.isWholesale) {
                val wholesalePrice = wholesalePriceMap[info.cartItem.cartItemId]
                wholesalePrice?.toInt() ?: info.cartItem.price
            } else {
                info.cartItem.price
            }
        }.toIntArray()

        CheckoutActivity.startForCart(this, cartItemIds, wholesaleArray, wholesalePricesArray)
    }

    private fun observeViewModel() {
        viewModel.cartItems.observe(this) { cartItems ->
            if (cartItems.isNullOrEmpty()) {
                binding.emptyCart.visibility = View.VISIBLE
                showEmptyState(true)

            } else {
                showEmptyState(false)
                storeAdapter.submitList(cartItems)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarCart?.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d("CartActivity", "Loading state: $isLoading")
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.emptyCart.visibility = View.VISIBLE
                Log.e("CartActivity", "Error message: $it")
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

        viewModel.hasConsistentWholesaleStatus.observe(this) { isConsistent ->
            if (!isConsistent && (viewModel.totalSelectedCount.value ?: 0) > 1) {
                binding.btnCheckout.isEnabled = false
                // Show an error message or indicator
                binding.tvWholesaleWarning.visibility = View.VISIBLE
                binding.tvWholesaleWarning.text = "Tidak dapat checkout produk grosir dan retail sekaligus"
            } else {
                binding.btnCheckout.isEnabled = true
                binding.tvWholesaleWarning.visibility = View.GONE
            }
        }

        viewModel.cartItemWholesaleStatus.observe(this) { wholesaleStatusMap ->
            viewModel.cartItemWholesalePrice.value?.let { wholesalePriceMap ->
                storeAdapter.updateWholesaleStatus(wholesaleStatusMap, wholesalePriceMap)
            }
        }

        viewModel.cartItemWholesalePrice.observe(this) { wholesalePriceMap ->
            viewModel.cartItemWholesaleStatus.value?.let { wholesaleStatusMap ->
                storeAdapter.updateWholesaleStatus(wholesaleStatusMap, wholesalePriceMap)
            }
        }

        viewModel.productImages.observe(this) { productImages ->
            storeAdapter.updateProductImages(productImages)
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