package com.alya.ecommerce_serang.ui.order

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.api.dto.CheckoutData
import com.alya.ecommerce_serang.data.api.dto.OrderRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.ActivityCheckoutBinding
import com.alya.ecommerce_serang.databinding.ActivityDetailProductBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.google.gson.Gson
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var itemOrderAdapter: CheckoutSellerAdapter? = null

    private val viewModel: CheckoutViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            val orderRepository = OrderRepository(apiService)
            CheckoutViewModel(orderRepository)
        }
    }

    private var orderRequest: OrderRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        // Get order request from intent
        getOrderRequestFromIntent()

        // Setup UI components
        setupToolbar()
        setupObservers()
        setupClickListeners()

        // Load data if order request is available
        orderRequest?.let {
            viewModel.loadCheckoutData(it)
            // Update shipping method display
            binding.tvShippingMethod.text = "${it.shipName} ${it.shipService} (${it.shipEtd} hari)"
        } ?: run {
            // Handle case when order request is not available
            Toast.makeText(this, "Error: Order request data not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getOrderRequestFromIntent() {
        // Check for direct OrderRequest object
        if (intent.hasExtra(EXTRA_ORDER_REQUEST)) {
            orderRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_ORDER_REQUEST, OrderRequest::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_ORDER_REQUEST) as? OrderRequest
            }
        }
        // Check for JSON string
        else if (intent.hasExtra(EXTRA_ORDER_REQUEST_JSON)) {
            val jsonString = intent.getStringExtra(EXTRA_ORDER_REQUEST_JSON)
            try {
                orderRequest = Gson().fromJson(jsonString, OrderRequest::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing order request JSON", e)
            }
        }
        // Check for individual fields
        else if (intent.hasExtra(EXTRA_ADDRESS_ID) && intent.hasExtra(EXTRA_PRODUCT_ID)) {
            orderRequest = OrderRequest(
                address_id = intent.getIntExtra(EXTRA_ADDRESS_ID, 0),
                payment_method_id = intent.getIntExtra(EXTRA_PAYMENT_METHOD_ID, 0),
                ship_price = intent.getIntExtra(EXTRA_SHIP_PRICE, 0),
                ship_name = intent.getStringExtra(EXTRA_SHIP_NAME) ?: "",
                ship_service = intent.getStringExtra(EXTRA_SHIP_SERVICE) ?: "",
                is_negotiable = intent.getBooleanExtra(EXTRA_IS_NEGOTIABLE, false),
                product_id = intent.getIntExtra(EXTRA_PRODUCT_ID, 0),
                quantity = intent.getIntExtra(EXTRA_QUANTITY, 0),
                ship_etd = intent.getStringExtra(EXTRA_SHIP_ETD) ?: ""
            )
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
            setupSellerOrderRecyclerView(data)
            updateOrderSummary()
        }

        // Observe address details
        viewModel.addressDetails.observe(this) { address ->
            binding.tvPlacesAddress.text = address.label
            binding.tvAddress.text = address.fullAddress
        }

        // Observe payment details
        viewModel.paymentDetails.observe(this) { payment ->
            binding.tvPaymentMethod.text = payment.name
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
            // binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupSellerOrderRecyclerView(checkoutData: CheckoutData) {
        val adapter = CheckoutSellerAdapter(checkoutData)
        binding.rvSellerOrder.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            this.adapter = adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun updateOrderSummary() {
        viewModel.checkoutData.value?.let { data ->
            // Calculate subtotal (product price * quantity)
            val subtotal = data.productPrice * data.orderRequest.quantity
            binding.tvSubtotal.text = formatCurrency(subtotal)

            // Calculate total (subtotal + shipping)
            val total = subtotal + data.orderRequest.ship_price
            binding.tvTotal.text = formatCurrency(total)
        }
    }

    private fun setupClickListeners() {
        // Setup address selection
        binding.tvChangeAddress.setOnClickListener {
            // Launch address selection activity
            startActivityForResult(
                Intent(this, AddressSelectionActivity::class.java),
                REQUEST_ADDRESS
            )
        }

        // Setup payment button
        binding.btnPay.setOnClickListener {
            // Create the order by sending API request
            if (validateOrder()) {
                createOrder()
            }
        }

        // Setup voucher section
        binding.layoutVoucher.setOnClickListener {
            Toast.makeText(this, "Select Voucher", Toast.LENGTH_SHORT).show()
        }

        // Setup shipping method
        binding.layoutShippingMethod.setOnClickListener {
            // Launch shipping method selection
            val orderRequest = this.orderRequest ?: return@setOnClickListener
            val intent = Intent(this, ShippingMethodActivity::class.java)
            intent.putExtra(ShippingMethodActivity.EXTRA_PRODUCT_ID, orderRequest.product_id)
            startActivityForResult(intent, REQUEST_SHIPPING)
        }

        // Setup payment method
        binding.layoutPaymentMethod.setOnClickListener {
            // Launch payment method selection
            startActivityForResult(
                Intent(this, PaymentMethodActivity::class.java),
                REQUEST_PAYMENT
            )
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    private fun validateOrder(): Boolean {
        val orderRequest = this.orderRequest ?: return false

        // Check address
        if (orderRequest.address_id <= 0) {
            Toast.makeText(this, "Silakan pilih alamat pengiriman", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check shipping method
        if (orderRequest.ship_name.isEmpty() || orderRequest.ship_service.isEmpty()) {
            Toast.makeText(this, "Silakan pilih metode pengiriman", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check payment method
        if (orderRequest.payment_method_id <= 0) {
            Toast.makeText(this, "Silakan pilih metode pembayaran", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun createOrder() {
        val orderRequest = this.orderRequest ?: return

        // Show progress dialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Membuat pesanan...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // In a real app, you would send the order request to your API
        // For now, we'll simulate an API call
        Handler(Looper.getMainLooper()).postDelayed({
            progressDialog.dismiss()

            // Show success message
            Toast.makeText(this, "Pesanan berhasil dibuat!", Toast.LENGTH_SHORT).show()

            // Create intent result with the order request
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_ORDER_REQUEST, orderRequest)
            setResult(RESULT_OK, resultIntent)

            // Return to previous screen
            finish()
        }, 1500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADDRESS -> {
                    // Handle address selection result
                    val addressId = data?.getIntExtra(AddressSelectionActivity.EXTRA_ADDRESS_ID, 0) ?: 0
                    if (addressId > 0) {
                        orderRequest?.address_id = addressId
                        // Reload address details
                        orderRequest?.let { request ->
                            viewModelScope.launch {
                                val addressDetails = repository.getAddressDetails(request.address_id)
                                binding.tvPlacesAddress.text = addressDetails.label
                                binding.tvAddress.text = addressDetails.fullAddress
                            }
                        }
                    }
                }
                REQUEST_SHIPPING -> {
                    // Handle shipping method selection result
                    data?.let { intent ->
                        val shipName = intent.getStringExtra(ShippingMethodActivity.EXTRA_SHIP_NAME) ?: return
                        val shipService = intent.getStringExtra(ShippingMethodActivity.EXTRA_SHIP_SERVICE) ?: return
                        val shipPrice = intent.getIntExtra(ShippingMethodActivity.EXTRA_SHIP_PRICE, 0)
                        val shipEtd = intent.getStringExtra(ShippingMethodActivity.EXTRA_SHIP_ETD) ?: ""

                        // Update order request
                        orderRequest?.apply {
                            this.ship_name = shipName
                            this.ship_service = shipService
                            this.ship_price = shipPrice
                            this.ship_etd = shipEtd
                        }

                        // Update UI
                        binding.tvShippingMethod.text = "$shipName $shipService ($shipEtd hari)"
                        updateOrderSummary()
                    }
                }
                REQUEST_PAYMENT -> {
                    // Handle payment method selection result
                    val paymentMethodId = data?.getIntExtra(PaymentMethodActivity.EXTRA_PAYMENT_METHOD_ID, 0) ?: 0
                    if (paymentMethodId > 0) {
                        orderRequest?.payment_method_id = paymentMethodId
                        // Reload payment method details
                        orderRequest?.let { request ->
                            viewModelScope.launch {
                                val paymentDetails = repository.getPaymentMethodDetails(request.payment_method_id)
                                binding.tvPaymentMethod.text = paymentDetails.name
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CheckoutActivity"

        // Request codes
        const val REQUEST_ADDRESS = 100
        const val REQUEST_SHIPPING = 101
        const val REQUEST_PAYMENT = 102

        // Intent extras
        const val EXTRA_ORDER_REQUEST = "extra_order_request"
        const val EXTRA_ORDER_REQUEST_JSON = "extra_order_request_json"

        // Individual field extras
        const val EXTRA_ADDRESS_ID = "extra_address_id"
        const val EXTRA_PAYMENT_METHOD_ID = "extra_payment_method_id"
        const val EXTRA_SHIP_PRICE = "extra_ship_price"
        const val EXTRA_SHIP_NAME = "extra_ship_name"
        const val EXTRA_SHIP_SERVICE = "extra_ship_service"
        const val EXTRA_IS_NEGOTIABLE = "extra_is_negotiable"
        const val EXTRA_PRODUCT_ID = "extra_product_id"
        const val EXTRA_QUANTITY = "extra_quantity"
        const val EXTRA_SHIP_ETD = "extra_ship_etd"

        // Start methods for various ways to launch the activity

        // Start with OrderRequest object
        fun start(context: Context, orderRequest: OrderRequest) {
            val intent = Intent(context, CheckoutActivity::class.java)
            intent.putExtra(EXTRA_ORDER_REQUEST, orderRequest)
            context.startActivity(intent)
        }

        // Start with OrderRequest JSON
        fun startWithJson(context: Context, orderRequestJson: String) {
            val intent = Intent(context, CheckoutActivity::class.java)
            intent.putExtra(EXTRA_ORDER_REQUEST_JSON, orderRequestJson)
            context.startActivity(intent)
        }

        // Start with individual fields
        fun start(
            context: Context,
            addressId: Int,
            paymentMethodId: Int,
            shipPrice: Int,
            shipName: String,
            shipService: String,
            isNegotiable: Boolean,
            productId: Int,
            quantity: Int,
            shipEtd: String
        ) {
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra(EXTRA_ADDRESS_ID, addressId)
                putExtra(EXTRA_PAYMENT_METHOD_ID, paymentMethodId)
                putExtra(EXTRA_SHIP_PRICE, shipPrice)
                putExtra(EXTRA_SHIP_NAME, shipName)
                putExtra(EXTRA_SHIP_SERVICE, shipService)
                putExtra(EXTRA_IS_NEGOTIABLE, isNegotiable)
                putExtra(EXTRA_PRODUCT_ID, productId)
                putExtra(EXTRA_QUANTITY, quantity)
                putExtra(EXTRA_SHIP_ETD, shipEtd)
            }
            context.startActivity(intent)
        }

        // Launch for result with OrderRequest
        fun startForResult(activity: Activity, orderRequest: OrderRequest, requestCode: Int) {
            val intent = Intent(activity, CheckoutActivity::class.java)
            intent.putExtra(EXTRA_ORDER_REQUEST, orderRequest)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}