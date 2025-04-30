package com.alya.ecommerce_serang.ui.chat

import android.app.Activity
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
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.databinding.ActivityChatBinding
import com.alya.ecommerce_serang.ui.auth.LoginActivity
import com.alya.ecommerce_serang.ui.product.ProductUserViewModel
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.Constants
import com.alya.ecommerce_serang.utils.SessionManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    @Inject
    lateinit var sessionManager: SessionManager
    private lateinit var socketService: SocketIOService


    @Inject
    private lateinit var chatAdapter: ChatAdapter

    private val viewModel: ChatViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val userRepository = UserRepository(apiService)
            ChatViewModel(userRepository, socketService, sessionManager)
        }
    }

    // For image attachment
    private var tempImageUri: Uri? = null

    // Chat parameters from intent
    private var chatRoomId: Int = 0
    private var storeId: Int = 0
    private var productId: Int = 0

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

        // Get parameters from intent
        chatRoomId = intent.getIntExtra(Constants.EXTRA_CHAT_ROOM_ID, 0)
        storeId = intent.getIntExtra(Constants.EXTRA_STORE_ID, 0)
        productId = intent.getIntExtra(Constants.EXTRA_PRODUCT_ID, 0)

        // Check if user is logged in
        val userId = sessionManager.getUserId()
        val token = sessionManager.getToken()

        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "Chat Activity started - User ID: $userId, Chat Room: $chatRoomId")

        // Initialize ViewModel
        initViewModel()

        // Setup UI components
        setupRecyclerView()
        setupListeners()
        setupTypingIndicator()
        observeViewModel()
    }

    private fun initViewModel() {
        // Set chat parameters to ViewModel
        viewModel.setChatParameters(
            chatRoomId = chatRoomId,
            storeId = storeId,
            productId = productId,
            productName = intent.getStringExtra(Constants.EXTRA_PRODUCT_NAME) ?: "",
            productPrice = intent.getStringExtra(Constants.EXTRA_PRODUCT_PRICE) ?: "",
            productImage = intent.getStringExtra(Constants.EXTRA_PRODUCT_IMAGE) ?: "",
            productRating = intent.getFloatExtra(Constants.EXTRA_PRODUCT_RATING, 0f),
            storeName = intent.getStringExtra(Constants.EXTRA_STORE_NAME) ?: ""
        )
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
            if (message.isNotEmpty() || viewModel.state.value?.hasAttachment ?: false) {
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
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Update messages
                chatAdapter.submitList(state.messages)

                // Scroll to bottom if new message
                if (state.messages.isNotEmpty()) {
                    binding.recyclerChat.scrollToPosition(state.messages.size - 1)
                }

                // Update product info
                binding.tvProductName.text = state.productName
                binding.tvProductPrice.text = state.productPrice
                binding.ratingBar.rating = state.productRating
                binding.tvRating.text = state.productRating.toString()
                binding.tvSellerName.text = state.storeName

                // Load product image
                if (state.productImageUrl.isNotEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(BASE_URL + state.productImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imgProduct)
                }

                // Show/hide loading indicators
//                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnSend.isEnabled = !state.isSending

                // Update attachment hint
                if (state.hasAttachment) {
                    binding.editTextMessage.hint = getString(R.string.image_attached)
                } else {
                    binding.editTextMessage.hint = getString(R.string.write_message)
                }

                // Show typing indicator
                binding.tvTypingIndicator.visibility =
                    if (state.isOtherUserTyping) View.VISIBLE else View.GONE

                // Handle connection state
                handleConnectionState(state.connectionState)

                // Show error if any
                state.error?.let { error ->
                    Toast.makeText(this@ChatActivity, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun handleConnectionState(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                binding.tvConnectionStatus.visibility = View.GONE
            }
            is ConnectionState.Connecting -> {
                binding.tvConnectionStatus.visibility = View.VISIBLE
                binding.tvConnectionStatus.text = getString(R.string.connecting)
            }
            is ConnectionState.Disconnected -> {
                binding.tvConnectionStatus.visibility = View.VISIBLE
                binding.tvConnectionStatus.text = getString(R.string.disconnected_reconnecting)
            }
            is ConnectionState.Error -> {
                binding.tvConnectionStatus.visibility = View.VISIBLE
                binding.tvConnectionStatus.text = getString(R.string.connection_error, state.message)
            }
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
        // Get the file from Uri
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
        val filePath = cursor?.getString(columnIndex ?: 0)
        cursor?.close()

        if (filePath != null) {
            viewModel.setSelectedImageFile(File(filePath))
            Toast.makeText(this, R.string.image_selected, Toast.LENGTH_SHORT).show()
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
    }
}