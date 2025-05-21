package com.alya.ecommerce_serang.ui.chat

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.databinding.ActivityChatBinding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
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

//    // Chat parameters from intent
//    private var chatRoomId: Int = 0
//    private var storeId: Int = 0
//    private var productId: Int = 0

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

        // Setup UI components
        setupRecyclerView()
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
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
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
                viewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
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
    }

    private fun observeViewModel() {
        viewModel.chatRoomId.observe(this, Observer { chatRoomId ->
            if (chatRoomId > 0) {
                // Chat room has been created, now we can join the Socket.IO room
                viewModel.joinSocketRoom(chatRoomId)

                // Now we can also load chat history
                viewModel.loadChatHistory(chatRoomId)
                Log.d(TAG, "Chat Activity started - Chat Room: $chatRoomId")

            }
        })

        // Observe state changes using LiveData
        viewModel.state.observe(this, Observer { state ->
            // Update messages
            chatAdapter.submitList(state.messages)

            // Scroll to bottom if new message
            if (state.messages.isNotEmpty()) {
                binding.recyclerChat.scrollToPosition(state.messages.size - 1)
            }

            // Update product info
            if (!state.productName.isNullOrEmpty()) {
                binding.tvProductName.text = state.productName
                binding.tvProductPrice.text = state.productPrice
                binding.ratingBar.rating = state.productRating
                binding.tvRating.text = state.productRating.toString()
                binding.tvSellerName.text = state.storeName
                binding.tvStoreName.text=state.storeName

                // Load product image
                if (!state.productImageUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(BASE_URL + state.productImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imgProduct)
                }

                // Make sure the product section is visible
                binding.productContainer.visibility = View.VISIBLE
            } else {
                // Hide the product section if info is missing
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
                Toast.makeText(this@ChatActivity, error, Toast.LENGTH_SHORT).show()
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

            // First try the direct approach to get the file path
            var filePath: String? = null

            // For newer Android versions, we need to handle content URIs properly
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (columnIndex != -1) {
                            filePath = it.getString(columnIndex)
                            Log.d(TAG, "Found file path from cursor: $filePath")
                        }
                    }
                }

                // If we couldn't get the path directly, create a copy in our cache directory
                if (filePath == null) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val fileName = "img_${System.currentTimeMillis()}.jpg"
                        val outputFile = File(cacheDir, fileName)

                        outputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        filePath = outputFile.absolutePath
                        Log.d(TAG, "Created temp file from input stream: $filePath")
                    }
                }
            } else if (uri.scheme == "file") {
                // Direct file URI
                filePath = uri.path
                Log.d(TAG, "Got file path directly from URI: $filePath")
            }

            // Process the file path
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    // Check file size (limit to 5MB)
                    if (file.length() > 5 * 1024 * 1024) {
                        Toast.makeText(this, "Image too large (max 5MB), please select a smaller image", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Set the file to the ViewModel
                    viewModel.setSelectedImageFile(file)
                    Toast.makeText(this, R.string.image_selected, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully set image file: ${file.absolutePath}, size: ${file.length()} bytes")
                } else {
                    Log.e(TAG, "File does not exist: $filePath")
                    Toast.makeText(this, "Could not access the selected image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Could not get file path from URI: $uri")
                Toast.makeText(this, "Could not process the selected image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected image", e)
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
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
            storeImage: String? = null
        ) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORE_ID, storeId)
                putExtra(Constants.EXTRA_PRODUCT_ID, productId)
                putExtra(Constants.EXTRA_PRODUCT_NAME, productName)
                putExtra(Constants.EXTRA_PRODUCT_PRICE, productPrice)
                putExtra(Constants.EXTRA_PRODUCT_IMAGE, productImage)
                putExtra(Constants.EXTRA_STORE_IMAGE, storeImage)

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