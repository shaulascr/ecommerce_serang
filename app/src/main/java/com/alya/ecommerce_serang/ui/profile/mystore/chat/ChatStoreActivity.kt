package com.alya.ecommerce_serang.ui.profile.mystore.chat

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
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
import com.alya.ecommerce_serang.ui.chat.ChatAdapter
import com.alya.ecommerce_serang.ui.chat.ChatViewModel
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
class ChatStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var apiService: ApiService

    private lateinit var chatAdapter: ChatAdapter

    private val viewModel: ChatViewModel by viewModels()

    // For image attachment
    private var tempImageUri: Uri? = null

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

//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        enableEdgeToEdge()
//
//        // Apply insets to your root layout


        // Get parameters from intent
        val storeId = intent.getIntExtra(Constants.EXTRA_STORE_ID, 0)
        val userId = intent.getIntExtra(Constants.EXTRA_USER_ID, 0)
        val productId = intent.getIntExtra(Constants.EXTRA_PRODUCT_ID, 0)
        val productName = intent.getStringExtra(Constants.EXTRA_PRODUCT_NAME) ?: ""
        val productPrice = intent.getStringExtra(Constants.EXTRA_PRODUCT_PRICE) ?: ""
        val productImage = intent.getStringExtra(Constants.EXTRA_PRODUCT_IMAGE) ?: ""
        val productRating = intent.getFloatExtra(Constants.EXTRA_PRODUCT_RATING, 0f)
        val storeName = intent.getStringExtra(Constants.EXTRA_STORE_NAME) ?: ""
        val chatRoomId = intent.getIntExtra(Constants.EXTRA_CHAT_ROOM_ID, 0)
        val storeImg = intent.getStringExtra(Constants.EXTRA_STORE_IMAGE) ?: ""
        val userName = intent.getStringExtra(Constants.EXTRA_USER_NAME) ?: ""
        val userImg = intent.getStringExtra(Constants.EXTRA_USER_IMAGE) ?: ""

        // Check if user is logged in
        val token = sessionManager.getToken()

        if (token.isEmpty()) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvStoreName.text = userName
        val fullImageUrl = when (val img = userImg) {
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
        viewModel.setChatParametersStore(
            storeId = storeId,
            userId = userId,  // The user you want to chat with
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            productImage = productImage,
            productRating = productRating,
            storeName = storeName
        )


        // Then setup other components
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

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatStoreActivity)
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

//    private fun updateRecyclerViewPadding(inputLayoutBottomPadding: Int) {
//        // Calculate total bottom padding needed for RecyclerView
//        // This ensures the last message is visible above the input layout
//        val inputLayoutHeight = binding.layoutChatInput.height
//        val totalBottomPadding = inputLayoutHeight + inputLayoutBottomPadding
//
//        binding.recyclerChat.setPadding(
//            binding.recyclerChat.paddingLeft,
//            binding.recyclerChat.paddingTop,
//            binding.recyclerChat.paddingRight,
//            totalBottomPadding
//        )
//
//        // Scroll to bottom if there are messages
//        val messageCount = chatAdapter.itemCount
//        if (messageCount > 0) {
//            binding.recyclerChat.post {
//                binding.recyclerChat.scrollToPosition(messageCount - 1)
//            }
//        }
//    }

//        binding.recyclerChat.setPadding(
//            binding.recyclerChat.paddingLeft,
//            binding.recyclerChat.paddingTop,
//            binding.recyclerChat.paddingRight,
//            binding.layoutChatInput.height + binding.root.rootWindowInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
//        )

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

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Options button
        binding.btnOptions.setOnClickListener {
            showOptionsMenu()
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            val currentState = viewModel.state.value
            if (message.isNotEmpty() || (currentState != null && currentState.hasAttachment)) {
                viewModel.sendMessageStore(message)
                binding.editTextMessage.text.clear()

                // Instantly scroll to show new message
                binding.recyclerChat.postDelayed({
                    scrollToBottomInstant()
                }, 50)
            }
        }

        // Attachment button
        binding.btnAttachment.setOnClickListener {
            checkPermissionsAndShowImagePicker()
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

            // Update product info
            if (!state.productName.isNullOrEmpty()) {
                binding.tvProductName.text = state.productName
                binding.tvProductPrice.text = state.productPrice
                binding.ratingBar.rating = state.productRating
                binding.tvRating.text = state.productRating.toString()
                binding.tvSellerName.text = state.storeName
//                binding.tvStoreName.text = state.storeName

                val fullImageUrl = when (val img = state.productImageUrl) {
                    is String -> {
                        if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                    }
                    else -> R.drawable.placeholder_image
                }

                if (!state.productImageUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatStoreActivity)
                        .load(fullImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imgProduct)
                }

                binding.productContainer.visibility = View.VISIBLE
            } else {
                binding.productContainer.visibility = View.GONE
            }

            // Update attachment hint
            if (state.hasAttachment) {
                binding.editTextMessage.hint = getString(R.string.image_attached)
            } else {
                binding.editTextMessage.hint = getString(R.string.write_message)
            }

            // Show typing indicator
            binding.tvTypingIndicator.visibility =
                if (state.isOtherUserTyping) View.VISIBLE else View.GONE

            // Show error if any
            state.error?.let { error ->
                Toast.makeText(this@ChatStoreActivity, error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })
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
            Log.d(TAG, "Processing selected image: $uri")

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
            userId: Int,
            userName: String,
            userImg: String? = null
        ): Intent {
            return Intent(context, ChatStoreActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORE_ID, storeId)
                putExtra(Constants.EXTRA_PRODUCT_ID, productId)
                putExtra(Constants.EXTRA_PRODUCT_NAME, productName)
                putExtra(Constants.EXTRA_PRODUCT_PRICE, productPrice)
                putExtra(Constants.EXTRA_PRODUCT_IMAGE, productImage)
                putExtra(Constants.EXTRA_STORE_IMAGE, storeImage)
                putExtra(Constants.EXTRA_USER_ID, userId)
                putExtra(Constants.EXTRA_USER_NAME,userName)
                putExtra(Constants.EXTRA_USER_IMAGE, userImg)

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
        }
    }
}

//if implement typing status
//    private fun handleConnectionState(state: ConnectionState) {
//        when (state) {
//            is ConnectionState.Connected -> {
//                binding.tvConnectionStatus.visibility = View.GONE
//            }
//            is ConnectionState.Connecting -> {
//                binding.tvConnectionStatus.visibility = View.VISIBLE
//                binding.tvConnectionStatus.text = getString(R.string.connecting)
//            }
//            is ConnectionState.Disconnected -> {
//                binding.tvConnectionStatus.visibility = View.VISIBLE
//                binding.tvConnectionStatus.text = getString(R.string.disconnected_reconnecting)
//            }
//            is ConnectionState.Error -> {
//                binding.tvConnectionStatus.visibility = View.VISIBLE
//                binding.tvConnectionStatus.text = getString(R.string.connection_error, state.message)
//            }
//        }
//    }