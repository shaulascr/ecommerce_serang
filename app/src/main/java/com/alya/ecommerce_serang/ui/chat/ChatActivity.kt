package com.alya.ecommerce_serang.ui.chat

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.databinding.ActivityChatBinding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.utils.Constants
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var apiService: ApiService

    private lateinit var chatAdapter: ChatAdapter

    private val viewModel: ChatViewModel by viewModels()

    // For image attachment
    private var tempImageUri: Uri? = null

    private var imageAttach = false

    // Typing indicator handler
    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stopTypingRunnable = Runnable {
        viewModel.sendTypingStatus(false)
    }

    // Activity Result Launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempImageUri?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        apiService = ApiConfig.getApiService(sessionManager)

        Log.d("ChatActivity", "Token in storage: '${sessionManager.getToken()}'")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Get parameters from intent
        val storeId = intent.getIntExtra(Constants.EXTRA_STORE_ID, 0)
        val productId = intent.getIntExtra(Constants.EXTRA_PRODUCT_ID, 0)
        val productName = intent.getStringExtra(Constants.EXTRA_PRODUCT_NAME) ?: ""
        val productPrice = intent.getStringExtra(Constants.EXTRA_PRODUCT_PRICE) ?: ""
        val productImage = intent.getStringExtra(Constants.EXTRA_PRODUCT_IMAGE) ?: ""
        val productRating = intent.getFloatExtra(Constants.EXTRA_PRODUCT_RATING, 0f)
        val storeName = intent.getStringExtra(Constants.EXTRA_STORE_NAME) ?: ""
        val chatRoomId = intent.getIntExtra(Constants.EXTRA_CHAT_ROOM_ID, 0)
        val storeImg = intent.getStringExtra(Constants.EXTRA_STORE_IMAGE) ?: ""

        val shouldAttachProduct = intent.getBooleanExtra(Constants.EXTRA_ATTACH_PRODUCT, false)

        // Check if user is logged in
        val token = sessionManager.getToken()

        if (token.isEmpty()) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvStoreName.text = storeName
        val fullImageUrl = when (val img = storeImg) {
            is String -> {
                if (img.startsWith("/")) BASE_URL + img.substring(1) else img
            }
            else -> R.drawable.placeholder_image
        }

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(binding.imgProfile)

                // Set chat parameters to ViewModel
        viewModel.setChatParameters(
            storeId = storeId,
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            productImage = productImage,
            productRating = productRating,
            storeName = storeName
        )

        if (shouldAttachProduct && productId > 0) {
            viewModel.enableProductAttachment()
            showProductAttachmentToast()
        }

        // Setup UI components
        setupRecyclerView()
        setupWindowInsets()
        setupListeners()
        setupTypingIndicator()
        observeViewModel()

        // If opened from ChatListFragment with a valid chatRoomId
        if (chatRoomId > 0) {
            // Directly set the chatRoomId and load chat history
            viewModel._chatRoomId.value = chatRoomId
        }
    }

    private fun showProductAttachmentToast() {
        Toast.makeText(
            this,
            "Product will be attached to your message",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { productInfo ->
            // This lambda will be called when user taps on a product bubble
            handleProductClick(productInfo)
        }
        binding.recyclerChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity)
            // Use clipToPadding to allow content to scroll under padding
            clipToPadding = false
            // Set minimal padding - we'll handle spacing differently
            setPadding(paddingLeft, paddingTop, paddingRight, 16)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.chatToolbar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBarInsets.top)
            insets
        }

        // Handle IME (keyboard) and navigation bar insets for the input layout only
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutChatInput) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            Log.d(TAG, "Insets - IME: ${imeInsets.bottom}, NavBar: ${navBarInsets.bottom}")

            val bottomInset = if (imeInsets.bottom > 0) {
                imeInsets.bottom
            } else {
                navBarInsets.bottom
            }

            // Only apply padding to the input layout
            view.updatePadding(bottom = bottomInset)

            // When keyboard appears, scroll to bottom to keep last message visible
            if (imeInsets.bottom > 0) {
                // Keyboard is visible - scroll to bottom with delay to ensure layout is complete
                binding.recyclerChat.postDelayed({
                    scrollToBottomSmooth()
                }, 100)
            }

            insets
        }

        // Smooth animation for keyboard transitions
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.layoutChatInput,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeAnimation = runningAnimations.find {
                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
                    }

                    if (imeAnimation != null) {
                        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                        val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                        val targetBottomInset = if (imeInsets.bottom > 0) imeInsets.bottom else navBarInsets.bottom

                        // Only animate input layout padding
                        binding.layoutChatInput.updatePadding(bottom = targetBottomInset)
                    }

                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    // Smooth scroll to bottom after animation
                    scrollToBottomSmooth()
                }
            }
        )
    }


    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Options button
        binding.btnOptions.visibility = View.GONE
        binding.btnOptions.setOnClickListener {
            showOptionsMenu()
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            val currentState = viewModel.state.value
            if (message.isNotEmpty() || (currentState != null && currentState.hasAttachment)) {
                // This will automatically handle product attachment if enabled
                viewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
                binding.layoutAttachImage.visibility = View.GONE

                // Instantly scroll to show new message
                binding.recyclerChat.postDelayed({
                    scrollToBottomInstant()
                }, 50)
            }
        }

        // Attachment button
        binding.btnAttachment.setOnClickListener {
            this.currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
            checkPermissionsAndShowImagePicker()
        }

        binding.btnCloseChat.setOnClickListener{
            binding.layoutAttachImage.visibility = View.GONE
            imageAttach = false
            viewModel.clearSelectedImage()
        }

        // Product card click to enable/disable product attachment
        binding.productContainer.setOnClickListener {
            toggleProductAttachment()
        }
    }


    private fun toggleProductAttachment() {
        val currentState = viewModel.state.value
        if (currentState?.hasProductAttachment == true) {
            viewModel.disableProductAttachment()
            updateProductAttachmentUI(false)
            Toast.makeText(this, "Product attachment disabled", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.enableProductAttachment()
            updateProductAttachmentUI(true)
            Toast.makeText(this, "Product will be attached to your next message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProductAttachmentUI(isEnabled: Boolean) {
        if (isEnabled) {
            // Show visual indicator that product will be attached
            binding.productContainer.setBackgroundResource(R.drawable.bg_product_selected)
            binding.editTextMessage.hint = "Type your message (product will be attached)"
        } else {
            // Reset to normal state
            binding.productContainer.setBackgroundResource(R.drawable.bg_product_normal)
            binding.editTextMessage.hint = getString(R.string.write_message)
        }
    }

    private fun setupTypingIndicator() {
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.sendTypingStatus(true)

                // Reset the timer
                typingHandler.removeCallbacks(stopTypingRunnable)
                typingHandler.postDelayed(stopTypingRunnable, 1000)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Focus and show keyboard
        binding.editTextMessage.requestFocus()
        binding.editTextMessage.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextMessage, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun scrollToBottomSmooth() {
        val messageCount = chatAdapter.itemCount
        if (messageCount > 0) {
            binding.recyclerChat.post {
                // Use smooth scroll to bottom
                binding.recyclerChat.smoothScrollToPosition(messageCount - 1)
            }
        }
    }

    private fun scrollToBottomInstant() {
        val messageCount = chatAdapter.itemCount
        if (messageCount > 0) {
            binding.recyclerChat.post {
                // Instant scroll for new messages
                binding.recyclerChat.scrollToPosition(messageCount - 1)
            }
        }
    }

    // Extension function to make padding updates cleaner
    private fun View.updatePadding(
        left: Int = paddingLeft,
        top: Int = paddingTop,
        right: Int = paddingRight,
        bottom: Int = paddingBottom
    ) {
        setPadding(left, top, right, bottom)
    }

    private fun observeViewModel() {
        viewModel.chatRoomId.observe(this, Observer { chatRoomId ->
            if (chatRoomId > 0) {
                viewModel.joinSocketRoom(chatRoomId)
                viewModel.loadChatHistory(chatRoomId)
                Log.d(TAG, "Chat Activity started - Chat Room: $chatRoomId")
            }
        })

        viewModel.state.observe(this, Observer { state ->
            Log.d(TAG, "State updated - Messages: ${state.messages.size}")

            // Update messages
            val previousCount = chatAdapter.itemCount

            val displayItems = viewModel.getDisplayItems()

            chatAdapter.submitList(displayItems) {
                Log.d(TAG, "Messages submitted to adapter")
                // Only auto-scroll for new messages or initial load
                if (previousCount == 0 || state.messages.size > previousCount) {
                    scrollToBottomInstant()
                }
            }

            // layout attach product
            if (!state.productName.isNullOrEmpty()) {
                binding.tvProductName.text = state.productName
                binding.tvProductPrice.text = state.productPrice
                binding.ratingBar.rating = state.productRating
                binding.tvRating.text = state.productRating.toString()
                binding.tvSellerName.text = state.storeName
                binding.tvStoreName.text = state.storeName

                val fullImageUrl = when (val img = state.productImageUrl) {
                    is String -> {
                        if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                    }
                    else -> R.drawable.placeholder_image
                }

                if (!state.productImageUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(fullImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imgProduct)
                }
                updateProductCardUI(state.hasProductAttachment)

                binding.productContainer.visibility = View.GONE
            } else {
                binding.productContainer.visibility = View.GONE
            }

            updateInputHint(state)

            // Update attachment hint
            if (state.hasAttachment) {
                binding.layoutAttachImage.visibility = View.VISIBLE
            } else {
                binding.editTextMessage.hint = getString(R.string.write_message)
            }

            // Show error if any
            state.error?.let { error ->
                Toast.makeText(this@ChatActivity, error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })
    }

    private fun updateInputHint(state: ChatUiState) {
        binding.editTextMessage.hint = when {
            state.hasAttachment -> getString(R.string.write_message)
            state.hasProductAttachment -> "Type your message (product will be attached)"
            else -> getString(R.string.write_message)
        }
    }

    private fun updateProductCardUI(hasProductAttachment: Boolean) {
        if (hasProductAttachment) {
            // Show visual indicator that product will be attached
            binding.productContainer.setBackgroundResource(R.drawable.bg_product_selected)
        } else {
            // Reset to normal state
            binding.productContainer.setBackgroundResource(R.drawable.bg_product_normal)
        }
    }

    private fun handleProductClick(productInfo: ProductInfo) {
        // Navigate to product detail
        Toast.makeText(this, "Opening: ${productInfo.productName}", Toast.LENGTH_SHORT).show()

        // You can navigate to product detail here
         navigateToProductDetail(productInfo.productId)
    }

    private fun navigateToProductDetail(productId: Int) {
        try {
            val intent = Intent(this, DetailProductActivity::class.java).apply {
                putExtra("PRODUCT_ID", productId)
                // Add other necessary extras
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open product details", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error navigating to product detail", e)
        }
    }

    private fun showOptionsMenu() {
        val options = arrayOf(
            getString(R.string.block_user),
            getString(R.string.report),
            getString(R.string.clear_chat),
            getString(R.string.cancel)
        )


        AlertDialog.Builder(this)
            .setTitle(getString(R.string.options))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> Toast.makeText(this, R.string.block_user_selected, Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, R.string.report_selected, Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, R.string.clear_chat_selected, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkPermissionsAndShowImagePicker() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                Constants.REQUEST_STORAGE_PERMISSION
            )
        } else {
            showImagePickerOptions()
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_attachment))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_${timeStamp}.jpg"
        val storageDir = getExternalFilesDir(null)
        val imageFile = File(storageDir, imageFileName)

        tempImageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
        }

        takePictureLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            Log.d(TAG, "Processing selected image: ${uri.toString()}")
            imageAttach = true
            binding.layoutAttachImage.visibility = View.VISIBLE
            val fullImageUrl = when (val img = uri.toString()) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> R.drawable.placeholder_image
            }

            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.ivAttach)
            Log.d(TAG, "Display attach image: $uri")

            // Always use the copy-to-cache approach for reliability
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = "chat_img_${System.currentTimeMillis()}.jpg"
                val outputFile = File(cacheDir, fileName)

                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    if (outputFile.length() > 5 * 1024 * 1024) {
                        Log.e(TAG, "File too large: ${outputFile.length()} bytes")
                        Toast.makeText(this, "Image too large (max 5MB)", Toast.LENGTH_SHORT).show()
                        return
                    }

                    Log.d(TAG, "Image processed successfully: ${outputFile.absolutePath}, size: ${outputFile.length()}")
                    viewModel.setSelectedImageFile(outputFile)

                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to create image file")
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                Toast.makeText(this, "Could not access image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected image", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerOptions()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        typingHandler.removeCallbacks(stopTypingRunnable)
    }

    companion object {
        private const val TAG = "ChatActivity"

        /**
         * Create an intent to start the ChatActivity
         */
        fun createIntent(
            context: Activity,
            storeId: Int,
            productId: Int = 0,
            productName: String? = null,
            productPrice: String = "",
            productImage: String? = null,
            productRating: String? = null,
            storeName: String? = null,
            chatRoomId: Int = 0,
            storeImage: String? = null,
            attachProduct: Boolean = false // NEW: Flag to auto-attach product
        ) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORE_ID, storeId)
                putExtra(Constants.EXTRA_PRODUCT_ID, productId)
                putExtra(Constants.EXTRA_PRODUCT_NAME, productName)
                putExtra(Constants.EXTRA_PRODUCT_PRICE, productPrice)
                putExtra(Constants.EXTRA_PRODUCT_IMAGE, productImage)
                putExtra(Constants.EXTRA_STORE_IMAGE, storeImage)
                putExtra(Constants.EXTRA_ATTACH_PRODUCT, attachProduct) // NEW

                // Convert productRating string to float if provided
                if (productRating != null) {
                    try {
                        putExtra(Constants.EXTRA_PRODUCT_RATING, productRating.toFloat())
                    } catch (e: NumberFormatException) {
                        putExtra(Constants.EXTRA_PRODUCT_RATING, 0f)
                    }
                } else {
                    putExtra(Constants.EXTRA_PRODUCT_RATING, 0f)
                }

                putExtra(Constants.EXTRA_STORE_NAME, storeName)

                if (chatRoomId > 0) {
                    putExtra(Constants.EXTRA_CHAT_ROOM_ID, chatRoomId)
                }
            }
            context.startActivity(intent)
        }
    }
}