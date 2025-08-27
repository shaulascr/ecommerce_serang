package com.alya.ecommerce_serang.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.dto.OrderRequestBuy
import com.alya.ecommerce_serang.data.api.response.customer.product.DetailPaymentItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.databinding.ActivityCheckoutBinding
import com.alya.ecommerce_serang.ui.order.address.AddressActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.PopUpDialog
import com.alya.ecommerce_serang.utils.SessionManager
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var sessionManager: SessionManager
    private var paymentAdapter: PaymentMethodAdapter? = null
    private var cartCheckoutAdapter: CartCheckoutAdapter? = null
    private var checkoutSellerAdapter: CheckoutSellerAdapter? = null
    private var paymentMethodsLoaded = false

    private val viewModel: CheckoutViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            CheckoutViewModel(orderRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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

        // Setup UI components
        setupToolbar()
        setupObservers()
        setupClickListeners()
        processIntentData()
    }

    private fun processIntentData() {
        // Determine if this is Buy Now or Cart checkout
        val isBuyNow = intent.hasExtra(EXTRA_PRODUCT_ID) && !intent.hasExtra(EXTRA_CART_ITEM_IDS)
        val isWholesaleNow = intent.getBooleanExtra(EXTRA_ISWHOLESALE, false)

        if (isBuyNow) {
            // Process Buy Now flow
            viewModel.initializeBuyNow(
                storeId = intent.getIntExtra(EXTRA_STORE_ID, 0),
                storeName = intent.getStringExtra(EXTRA_STORE_NAME),
                productId = intent.getIntExtra(EXTRA_PRODUCT_ID, 0),
                productName = intent.getStringExtra(EXTRA_PRODUCT_NAME),
                productImage = intent.getStringExtra(EXTRA_PRODUCT_IMAGE),
                quantity = intent.getIntExtra(EXTRA_QUANTITY, 1),
                price = intent.getDoubleExtra(EXTRA_PRICE, 0.0),
                isWholesale = isWholesaleNow
            )
        } else {
            // Process Cart checkout flow
            val cartItemIds = intent.getIntArrayExtra(EXTRA_CART_ITEM_IDS)?.toList() ?: emptyList()
            val isWholesaleArray = intent.getBooleanArrayExtra(EXTRA_CART_ITEM_WHOLESALE)
            val wholesalePricesArray = intent.getIntArrayExtra(EXTRA_CART_ITEM_WHOLESALE_PRICES)

            if (cartItemIds.isNotEmpty()) {
                // Build map of cartItemId -> isWholesale
                val isWholesaleMap = if (isWholesaleArray != null && isWholesaleArray.size == cartItemIds.size) {
                    cartItemIds.mapIndexed { index, id ->
                        id to isWholesaleArray[index]
                    }.toMap()
                } else {
                    emptyMap()
                }

                // Build wholesalePriceMap - FIX: Map cartItemId to wholesale price
                val wholesalePriceMap = if (wholesalePricesArray != null && wholesalePricesArray.size == cartItemIds.size) {
                    cartItemIds.mapIndexed { index, id ->
                        id to wholesalePricesArray[index]
                    }.toMap()
                } else {
                    emptyMap()
                }

                viewModel.initializeFromCart(
                    cartItemIds,
                    isWholesaleMap,
                    wholesalePriceMap
                )

                Log.d("CheckoutActivity", "Cart IDs: $cartItemIds")
                Log.d("CheckoutActivity", "IsWholesaleArray: ${isWholesaleArray?.joinToString()}")
                Log.d("CheckoutActivity", "WholesalePricesArray: ${wholesalePricesArray?.joinToString()}")
                Log.d("CheckoutActivity", "IsWholesaleMap: $isWholesaleMap")
                Log.d("CheckoutActivity", "WholesalePriceMap: $wholesalePriceMap")
            } else {
                Toast.makeText(this, "Tidak ada item keranjang", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        // Observe checkout data
        viewModel.checkoutData.observe(this) { data ->
            setupProductRecyclerView(data)
            updateOrderSummary()

            if (data != null) {
                viewModel.getPaymentMethods()
            }
        }

        // Observe address details
        viewModel.addressDetails.observe(this) { address ->
            if (address != null) {
                // Show selected address
                binding.containerEmptyAddress.visibility = View.GONE
                binding.containerAddress.visibility = View.VISIBLE

                binding.tvPlacesAddress.text = address.recipient
                binding.tvAddress.text = "${address.street}, ${address.subdistrict}"
            } else {
                // Show empty address state
                binding.containerEmptyAddress.visibility = View.VISIBLE
                binding.containerAddress.visibility = View.GONE
            }
        }

        viewModel.availablePaymentMethods.observe(this) { paymentMethods ->
            if (paymentMethods.isNotEmpty() && !paymentMethodsLoaded) {
                Log.d("CheckoutActivity", "Setting up payment methods: ${paymentMethods.size} methods available")
                setupPaymentMethodsRecyclerView(paymentMethods)
                paymentMethodsLoaded = true
            }
        }

        viewModel.selectedPayment.observe(this) { selectedPayment ->
            if (selectedPayment != null) {
                Log.d("CheckoutActivity", "Observer notified of selected payment: ${selectedPayment.bankName}")

                // Update the adapter ONLY if it exists
                paymentAdapter?.let { adapter ->
                    adapter.setSelectedPaymentId(selectedPayment.id)
                    Log.d("CheckoutActivity", "Updated adapter with selected payment: ${selectedPayment.id}")
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnPay.isEnabled = !isLoading
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, "Terdapat kendala di pemesanan", Toast.LENGTH_SHORT).show()
                Log.e("CheckoutActivity", "Error from errorMessage: $message")
            }
        }

        // Observe order creation
        viewModel.orderCreated.observe(this) { created ->
            if (created) {
                Toast.makeText(this, "Berhasil membuat pesanan", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        viewModel.productImages.observe(this) { images ->
            Log.d("CheckoutActivity", "Product images updated: ${images.keys}")
            // Update adapter when images arrive
            cartCheckoutAdapter?.updateProductImages(images)
            checkoutSellerAdapter?.updateProductImages(images)
        }
    }

    private fun setupPaymentMethodsRecyclerView(paymentMethods: List<DetailPaymentItem>) {
        if (paymentMethods.isEmpty()) {
            Log.e("CheckoutActivity", "Payment methods list is empty")
            Toast.makeText(this, "Tidak ditemukan metode pembayaran", Toast.LENGTH_SHORT).show()

            // Show empty payment state
            binding.containerEmptyPayment.visibility = View.VISIBLE
            binding.rvPaymentInfo.visibility = View.GONE
            return
        }

        binding.containerEmptyPayment.visibility = View.GONE
        binding.rvPaymentInfo.visibility = View.VISIBLE

        // Debug logging
        Log.d("CheckoutActivity", "Setting up payment methods: ${paymentMethods.size} methods available")

        if (paymentAdapter == null) {
            paymentAdapter = PaymentMethodAdapter(paymentMethods) { payment ->
                Log.d("CheckoutActivity", "Payment selected in adapter: ${payment.bankName}")

                // Set this payment as selected in the ViewModel
                viewModel.setPaymentMethod(payment.id)
            }

            binding.rvPaymentInfo.apply {
                layoutManager = LinearLayoutManager(this@CheckoutActivity)
                adapter = paymentAdapter
            }
        }
    }

    private fun updatePaymentMethodsAdapter(paymentMethods: List<DetailPaymentItem>, selectedId: Int?) {
        Log.d("CheckoutActivity", "Updating payment adapter with ${paymentMethods.size} methods")

        // Simple test adapter
        val testAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val textView = TextView(parent.context)
                textView.setPadding(16, 16, 16, 16)
                textView.textSize = 16f
                return object : RecyclerView.ViewHolder(textView) {}
            }

            override fun getItemCount() = paymentMethods.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val payment = paymentMethods[position]
                (holder.itemView as TextView).text = "Payment: ${payment.bankName}"
            }
        }

        binding.rvPaymentInfo.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = testAdapter
        }
    }

    private fun setupProductRecyclerView(checkoutData: CheckoutData) {
        if (checkoutData.isBuyNow || checkoutData.cartItems.size <= 1) {
            Log.d("CheckoutActivity", "Using CheckoutSellerAdapter")
            val adapter = CheckoutSellerAdapter(checkoutData)

            // Keep reference for image updates - create a field in your activity
            checkoutSellerAdapter = adapter

            binding.rvProductItems.apply {
                layoutManager = LinearLayoutManager(this@CheckoutActivity)
                this.adapter = adapter
                isNestedScrollingEnabled = false
            }

            // Load images for cart items
            if (!checkoutData.isBuyNow) {
                checkoutData.cartItems.forEach { item ->
                    viewModel.loadProductImage(item.productId)
                }
            }
        } else {
            Log.d("CheckoutActivity", "Using CartCheckoutAdapter")
            Log.d("CheckoutActivity", "Cart items count: ${checkoutData.cartItems.size}")

            // Create adapter and keep reference
            cartCheckoutAdapter = CartCheckoutAdapter(checkoutData)

            binding.rvProductItems.apply {
                layoutManager = LinearLayoutManager(this@CheckoutActivity)
                adapter = cartCheckoutAdapter
                isNestedScrollingEnabled = false
            }

            // Load images for each product
            checkoutData.cartItems.forEach { item ->
                Log.d("CheckoutActivity", "Loading image for productId: ${item.productId}")
                viewModel.loadProductImage(item.productId)
            }
        }

        binding.containerEmptyProducts.visibility = View.GONE
        binding.rvProductItems.visibility = View.VISIBLE
    }

    private fun updateOrderSummary() {
        viewModel.checkoutData.value?.let { data ->
            // Update price information
            binding.tvItemTotal.text = formatCurrency(viewModel.calculateSubtotal())

            // Get shipping price
            val shipPrice = if (data.isBuyNow) {
                (data.orderRequest as OrderRequestBuy).shipPrice.toDouble()
            } else {
                (data.orderRequest as OrderRequest).shipPrice.toDouble()
            }
            binding.tvShippingFee.text = formatCurrency(shipPrice)

            // Update total
            val total = viewModel.calculateTotal()
            binding.tvTotal.text = formatCurrency(total)
            binding.tvBottomTotal.text = formatCurrency(total)
        }
    }

    private fun updateShippingUI(shipName: String, shipService: String, shipEtd: String, shipPrice: Int) {
        if (shipName.isNotEmpty() && shipService.isNotEmpty()) {
            // Hide empty state and show selected shipping
            binding.containerEmptyShipping.visibility = View.GONE
            binding.cardShipment.visibility = View.VISIBLE

            binding.tvCourierName.text = "$shipName $shipService"
            binding.tvDeliveryEstimate.text = "$shipEtd hari kerja"
            binding.tvShippingPrice.text = formatCurrency(shipPrice.toDouble())
            binding.rbJne.isChecked = true
        } else {
            // Show empty shipping state
            binding.containerEmptyShipping.visibility = View.VISIBLE
            binding.cardShipment.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        // Address selection
        binding.tvChangeAddress.setOnClickListener {
            val intent = Intent(this, AddressActivity::class.java)
            addressSelectionLauncher.launch(intent)
        }

        // Shipping method selection
        binding.tvShippingOption.setOnClickListener {
            val addressId = viewModel.addressDetails.value?.id ?: 0
            if (addressId <= 0) {
                Toast.makeText(this, "Silahkan pilih alamat dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch shipping selection with address and product info
            val intent = Intent(this, ShippingActivity::class.java)
            intent.putExtra(ShippingActivity.EXTRA_ADDRESS_ID, addressId)

            // Add product info for courier cost calculation
            val currentData = viewModel.checkoutData.value
            if (currentData != null) {
                if (currentData.isBuyNow) {
                    val buyRequest = currentData.orderRequest as OrderRequestBuy
                    intent.putExtra(ShippingActivity.EXTRA_PRODUCT_ID, buyRequest.productId)
                    intent.putExtra(ShippingActivity.EXTRA_QUANTITY, buyRequest.quantity)
                } else {
                    // For cart, we'll pass the first item's info
                    val firstItem = currentData.cartItems.firstOrNull()
                    if (firstItem != null) {
                        intent.putExtra(ShippingActivity.EXTRA_PRODUCT_ID, firstItem.productId)
                        intent.putExtra(ShippingActivity.EXTRA_QUANTITY, firstItem.quantity)
                    }
                }
            }

            shippingSelectionLauncher.launch(intent)
        }

        // Create order button
        binding.btnPay.setOnClickListener {
            if (validateOrder()) {
                PopUpDialog.showConfirmDialog(
                    context = this,
                    title = "Apakah anda yakin membuat pesanan?",
                    message = "Pastikan data yang dimasukkan sudah benar",
                    positiveText = "Ya",
                    negativeText = "Tidak",
                    onYesClicked = {
                        viewModel.createOrder()
                    }
                )
            }
        }

//        // Voucher section (if implemented)
//        binding.layoutVoucher?.setOnClickListener {
//            Toast.makeText(this, "Voucher feature not implemented", Toast.LENGTH_SHORT).show()
//        }
    }

    private val addressSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val addressId = result.data?.getIntExtra(AddressActivity.EXTRA_ADDRESS_ID, 0) ?: 0
            if (addressId > 0) {
                viewModel.setSelectedAddress(addressId)

                // You might want to show a toast or some UI feedback
                Toast.makeText(this, "Berhasil memilih alamat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val shippingSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val shipName = data.getStringExtra(ShippingActivity.EXTRA_SHIP_NAME) ?: return@registerForActivityResult
            val shipService = data.getStringExtra(ShippingActivity.EXTRA_SHIP_SERVICE) ?: return@registerForActivityResult
            val shipPrice = data.getIntExtra(ShippingActivity.EXTRA_SHIP_PRICE, 0)
            val shipEtd = data.getStringExtra(ShippingActivity.EXTRA_SHIP_ETD) ?: ""

            // Update shipping in ViewModel
            viewModel.setShippingMethod(shipName, shipService, shipPrice, shipEtd)

            // Update UI - display shipping name and service in one line
            updateShippingUI(shipName, shipService, shipEtd, shipPrice)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    private fun validateOrder(): Boolean {
        // Check if address is selected
        if (viewModel.addressDetails.value == null) {
            Toast.makeText(this, "Silakan pilih alamat pengiriman", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if shipping is selected
        val checkoutData = viewModel.checkoutData.value ?: return false
        val shipName = if (checkoutData.isBuyNow) {
            (checkoutData.orderRequest as OrderRequestBuy).shipName
        } else {
            (checkoutData.orderRequest as OrderRequest).shipName
        }

        if (shipName.isEmpty()) {
            Toast.makeText(this, "Silakan pilih metode pengiriman", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if payment method is selected
        val paymentMethodId = if (checkoutData.isBuyNow) {
            (checkoutData.orderRequest as OrderRequestBuy).paymentMethodId
        } else {
            (checkoutData.orderRequest as OrderRequest).paymentMethodId
        }

        if (paymentMethodId <= 0) {
            Toast.makeText(this, "Silakan pilih metode pembayaran", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    companion object {
        // Intent extras
        const val EXTRA_CART_ITEM_IDS = "extra_cart_item_ids"
        const val EXTRA_STORE_ID = "STORE_ID"
        const val EXTRA_STORE_NAME = "STORE_NAME"
        const val EXTRA_PRODUCT_ID = "PRODUCT_ID"
        const val EXTRA_PRODUCT_NAME = "PRODUCT_NAME"
        const val EXTRA_PRODUCT_IMAGE = "PRODUCT_IMAGE"
        const val EXTRA_QUANTITY = "QUANTITY"
        const val EXTRA_PRICE = "PRICE"
        const val EXTRA_ISWHOLESALE = "ISWHOLESALE"
        const val EXTRA_CART_ITEM_WHOLESALE = "EXTRA_CART_ITEM_WHOLESALE"
        const val EXTRA_CART_ITEM_WHOLESALE_PRICES = "EXTRA_CART_ITEM_WHOLESALE_PRICES"

        // Helper methods for starting activity

        // TO DO: delete iswholesale klo ngga dibuthin
        // For Buy Now
        fun startForBuyNow(
            context: Context,
            storeId: Int,
            storeName: String?,
            productId: Int,
            productName: String?,
            productImage: String?,
            quantity: Int,
            price: Double,
            isWholesale: Boolean
        ) {
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra(EXTRA_STORE_ID, storeId)
                putExtra(EXTRA_STORE_NAME, storeName)
                putExtra(EXTRA_PRODUCT_ID, productId)
                putExtra(EXTRA_PRODUCT_NAME, productName)
                putExtra(EXTRA_PRODUCT_IMAGE, productImage)
                putExtra(EXTRA_QUANTITY, quantity)
                putExtra(EXTRA_PRICE, price)
                putExtra(EXTRA_ISWHOLESALE, isWholesale)
            }
            context.startActivity(intent)
        }

        // For Cart checkout
        fun startForCart(
            context: Context,
            cartItemIds: List<Int>,
            isWholesaleArray: BooleanArray? = null,
            wholesalePrices: IntArray? = null
        ) {
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra(EXTRA_CART_ITEM_IDS, cartItemIds.toIntArray())
                if (isWholesaleArray != null) {
                    putExtra(EXTRA_CART_ITEM_WHOLESALE, isWholesaleArray)
                }
                if (wholesalePrices != null) {
                    putExtra(EXTRA_CART_ITEM_WHOLESALE_PRICES, wholesalePrices)
                }
            }
            context.startActivity(intent)
        }
    }
}