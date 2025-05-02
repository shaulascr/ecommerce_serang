//package com.alya.ecommerce_serang.ui.chat
//
//import android.Manifest
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Bundle
//import android.provider.MediaStore
//import android.text.Editable
//import android.text.TextWatcher
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import androidx.fragment.app.viewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.navArgs
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.alya.ecommerce_serang.BuildConfig.BASE_URL
//import com.alya.ecommerce_serang.R
//import com.alya.ecommerce_serang.databinding.FragmentChatBinding
//import com.alya.ecommerce_serang.utils.Constants
//import com.bumptech.glide.Glide
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//@AndroidEntryPoint
//class ChatFragment : Fragment() {
//
//    private var _binding: FragmentChatBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: ChatViewModel by viewModels()
////    private val args: ChatFragmentArgs by navArgs()
//
//    private lateinit var chatAdapter: ChatAdapter
//
//    // For image attachment
//    private var tempImageUri: Uri? = null
//
//    // Typing indicator handler
//    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
//    private val stopTypingRunnable = Runnable {
//        viewModel.sendTypingStatus(false)
//    }
//
//    // Activity Result Launchers
//    private val pickImageLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            result.data?.data?.let { uri ->
//                handleSelectedImage(uri)
//            }
//        }
//    }
//
//    private val takePictureLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            tempImageUri?.let { uri ->
//                handleSelectedImage(uri)
//            }
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentChatBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupRecyclerView()
//        setupListeners()
//        setupTypingIndicator()
//        observeViewModel()
//    }
//
//    private fun setupRecyclerView() {
//        chatAdapter = ChatAdapter()
//        binding.recyclerChat.apply {
//            adapter = chatAdapter
//            layoutManager = LinearLayoutManager(requireContext()).apply {
//                stackFromEnd = true
//            }
//        }
//    }
//
//    private fun setupListeners() {
//        // Back button
//        binding.btnBack.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
//
//        // Options button
//        binding.btnOptions.setOnClickListener {
//            showOptionsMenu()
//        }
//
//        // Send button
//        binding.btnSend.setOnClickListener {
//            val message = binding.editTextMessage.text.toString().trim()
//            if (message.isNotEmpty() || viewModel.state.value.hasAttachment) {
//                viewModel.sendMessage(message)
//                binding.editTextMessage.text.clear()
//            }
//        }
//
//        // Attachment button
//        binding.btnAttachment.setOnClickListener {
//            checkPermissionsAndShowImagePicker()
//        }
//    }
//
//    private fun setupTypingIndicator() {
//        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                viewModel.sendTypingStatus(true)
//
//                // Reset the timer
//                typingHandler.removeCallbacks(stopTypingRunnable)
//                typingHandler.postDelayed(stopTypingRunnable, 1000)
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//    }
//
//    private fun observeViewModel() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewModel.state.collectLatest { state ->
//                // Update messages
//                chatAdapter.submitList(state.messages)
//
//                // Scroll to bottom if new message
//                if (state.messages.isNotEmpty()) {
//                    binding.recyclerChat.scrollToPosition(state.messages.size - 1)
//                }
//
//                // Update product info
//                binding.tvProductName.text = state.productName
//                binding.tvProductPrice.text = state.productPrice
//                binding.ratingBar.rating = state.productRating
//                binding.tvRating.text = state.productRating.toString()
//                binding.tvSellerName.text = state.storeName
//
//                // Load product image
//                if (state.productImageUrl.isNotEmpty()) {
//                    Glide.with(requireContext())
//                        .load(BASE_URL + state.productImageUrl)
//                        .centerCrop()
//                        .placeholder(R.drawable.placeholder_image)
//                        .error(R.drawable.placeholder_image)
//                        .into(binding.imgProduct)
//                }
//
//                // Show/hide loading indicators
//                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
//                binding.btnSend.isEnabled = !state.isSending
//
//                // Update attachment hint
//                if (state.hasAttachment) {
//                    binding.editTextMessage.hint = getString(R.string.image_attached)
//                } else {
//                    binding.editTextMessage.hint = getString(R.string.write_message)
//                }
//
//                // Show typing indicator
//                binding.tvTypingIndicator.visibility =
//                    if (state.isOtherUserTyping) View.VISIBLE else View.GONE
//
//                // Handle connection state
//                handleConnectionState(state.connectionState)
//
//                // Show error if any
//                state.error?.let { error ->
//                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
//                    viewModel.clearError()
//                }
//            }
//        }
//    }
//
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
//
//    private fun showOptionsMenu() {
//        val options = arrayOf(
//            getString(R.string.block_user),
//            getString(R.string.report),
//            getString(R.string.clear_chat),
//            getString(R.string.cancel)
//        )
//
//        androidx.appcompat.app.AlertDialog.Builder(requireContext())
//            .setTitle(getString(R.string.options))
//            .setItems(options) { dialog, which ->
//                when (which) {
//                    0 -> Toast.makeText(requireContext(), R.string.block_user_selected, Toast.LENGTH_SHORT).show()
//                    1 -> Toast.makeText(requireContext(), R.string.report_selected, Toast.LENGTH_SHORT).show()
//                    2 -> Toast.makeText(requireContext(), R.string.clear_chat_selected, Toast.LENGTH_SHORT).show()
//                }
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//    private fun checkPermissionsAndShowImagePicker() {
//        if (ContextCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                requireActivity(),
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
//                Constants.REQUEST_STORAGE_PERMISSION
//            )
//        } else {
//            showImagePickerOptions()
//        }
//    }
//
//    private fun showImagePickerOptions() {
//        val options = arrayOf(
//            getString(R.string.take_photo),
//            getString(R.string.choose_from_gallery),
//            getString(R.string.cancel)
//        )
//
//        androidx.appcompat.app.AlertDialog.Builder(requireContext())
//            .setTitle(getString(R.string.select_attachment))
//            .setItems(options) { dialog, which ->
//                when (which) {
//                    0 -> openCamera()
//                    1 -> openGallery()
//                }
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//    private fun openCamera() {
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        val imageFileName = "IMG_${timeStamp}.jpg"
//        val storageDir = requireContext().getExternalFilesDir(null)
//        val imageFile = File(storageDir, imageFileName)
//
//        tempImageUri = FileProvider.getUriForFile(
//            requireContext(),
//            "${requireContext().packageName}.fileprovider",
//            imageFile
//        )
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
//        }
//
//        takePictureLauncher.launch(intent)
//    }
//
//    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        pickImageLauncher.launch(intent)
//    }
//
//    private fun handleSelectedImage(uri: Uri) {
//        // Get the file from Uri
//        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
//        val cursor = requireContext().contentResolver.query(uri, filePathColumn, null, null, null)
//        cursor?.moveToFirst()
//        val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
//        val filePath = cursor?.getString(columnIndex ?: 0)
//        cursor?.close()
//
//        if (filePath != null) {
//            viewModel.setSelectedImageFile(File(filePath))
//            Toast.makeText(requireContext(), R.string.image_selected, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == Constants.REQUEST_STORAGE_PERMISSION) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                showImagePickerOptions()
//            } else {
//                Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        typingHandler.removeCallbacks(stopTypingRunnable)
//        _binding = null
//    }
//}