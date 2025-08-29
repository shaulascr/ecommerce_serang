package com.alya.ecommerce_serang.ui.order.detail

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.AddEvidenceMultipartRequest
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityAddEvidencePaymentBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.PopUpDialog
import com.alya.ecommerce_serang.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEvidencePaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEvidencePaymentBinding
    private lateinit var sessionManager: SessionManager
    private var orderId: Int = 0
    private var paymentInfoId: Int = 0
    private lateinit var productPrice: String
    private var selectedImageUri: Uri? = null


    private val viewModel: PaymentViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val orderRepository = OrderRepository(apiService)
            PaymentViewModel(orderRepository)
        }
    }

    private val paymentMethods = arrayOf(
        "Pilih Metode Pembayaran",
        "Transfer Bank",
    )

//    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let {
//            selectedImageUri = it
//            binding.ivUploadedImage.setImageURI(selectedImageUri)
//            binding.ivUploadedImage.visibility = View.VISIBLE
//            binding.layoutUploadPlaceholder.visibility = View.GONE
//        }
//    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityAddEvidencePaymentBinding.inflate(layoutInflater)
            setContentView(binding.root)

            sessionManager = SessionManager(this)

            intent.extras?.let { bundle ->
                orderId = bundle.getInt("ORDER_ID", 0)
                paymentInfoId = bundle.getInt("PAYMENT_INFO_ID", 0)
                productPrice = intent.getStringExtra("TOTAL_AMOUNT") ?: "Rp0"
                Log.d(TAG, "Intent data: OrderID=$orderId, PaymentInfoId=$paymentInfoId, Price=$productPrice")
            }

            WindowCompat.setDecorFitsSystemWindows(window, false)
            enableEdgeToEdge()

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                windowInsets
            }

            Log.d(TAG, "7. About to setup toolbar - COMMENTING OUT PROBLEMATIC LINE")
            // COMMENT OUT THIS LINE TEMPORARILY:
//             binding.toolbar.navigationIcon.apply { finish() }

            setupUI()

            viewModel.getOrderDetails(orderId)

            setupListeners()
            setupObservers()

        } catch (e: Exception) {
            Log.e(TAG, "ERROR in AddEvidencePaymentActivity onCreate: ${e.message}", e)
        }
    }

    private fun setupUI() {
        val paymentMethods = listOf("Transfer Bank", "QRIS")
        val adapter = SpinnerCardAdapter(this, paymentMethods)
        binding.spinnerPaymentMethod.adapter = adapter
    }

    private fun setupListeners() {

        // Upload image button
        binding.tvAddPhoto.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        binding.frameUploadImage.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        // Date picker
        binding.tvPaymentDate.setOnClickListener {
            showDatePicker()
        }

        // Payment method spinner
        binding.spinnerPaymentMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Skip the hint (first item)
                if (position > 0) {
                    val selectedMethod = paymentMethods[position]

                    // You can also use it for further processing
                    Log.d(TAG, "Selected payment method: $selectedMethod")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {

            validateAndUpload()
            Log.d(TAG, "AddEvidencePaymentActivity onCreate completed")
        }
    }

    private fun setupObservers() {
        viewModel.uploadResult.observe(this) { result ->
            when (result) {
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    Toast.makeText(this, "Bukti pembayaran berhasil dikirim", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Upload successful: ${result.data}")
                    // Navigate back or to confirmation screen
                    finish()
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    Log.e(TAG, "Upload failed: ${result.exception.message}")
                    Toast.makeText(this, "Gagal mengirim bukti pembayaran: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                    Log.d(TAG, "Uploading payment proof...")
                }
            }
        }
    }

    private fun checkPermissionsAndShowImagePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_STORAGE_PERMISSION)
            } else {
                showImagePickerOptions()
            }
        } else {
            // For older versions, use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
            } else {
                showImagePickerOptions()
            }
        }
    }

    // Exact same approach as ChatActivity
    private fun showImagePickerOptions() {
        val options = arrayOf(
            "Pilih dari Galeri",
            "Kembali"
        )

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_dialog_add_evidence, R.id.tvOption, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                return view
            }
        }

        AlertDialog.Builder(this)
            .setAdapter(adapter) { dialog, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    // Using the same gallery opening method as ChatActivity
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery", e)
            Toast.makeText(this, "Gagal membuka galeri", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            Log.d(TAG, "Processing selected image: $uri")

            // Use the same copy-to-cache approach as ChatActivity
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = "evidence_${System.currentTimeMillis()}.jpg"
                val outputFile = File(cacheDir, fileName)

                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    // Check file size (max 5MB like ChatActivity)
                    if (outputFile.length() > 5 * 1024 * 1024) {
                        Log.e(TAG, "File too large: ${outputFile.length()} bytes")
                        Toast.makeText(this, "Gambar terlalu besar (maksimal 5MB)", Toast.LENGTH_SHORT).show()
                        outputFile.delete()
                        return
                    }

                    // Success - update UI
                    selectedImageUri = Uri.fromFile(outputFile)
                    binding.ivUploadedImage.setImageURI(selectedImageUri)
                    binding.ivUploadedImage.visibility = View.VISIBLE
                    binding.layoutUploadPlaceholder.visibility = View.GONE

                    Log.d(TAG, "Image processed successfully: ${outputFile.absolutePath}, size: ${outputFile.length()}")
                    Toast.makeText(this, "Gambar berhasil dipilih", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to create image file")
                    Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                Toast.makeText(this, "Tidak dapat mengakses gambar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected image", e)
            Toast.makeText(this, "Terjadi kendala", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerOptions()
            } else {
                Toast.makeText(this, "Izin diperlukan untuk mengakses galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateAndUpload() {
        // Validate all fields
        if (selectedImageUri == null) {
            Toast.makeText(this, "Silahkan pilih bukti pembayaran", Toast.LENGTH_SHORT).show()
            return
        }

        //in case applied metode pembayaran yang lain
//        if (binding.spinnerPaymentMethod.selectedItemPosition == 0) {
//            Toast.makeText(this, "Silahkan pilih metode pembayaran", Toast.LENGTH_SHORT).show()
//            return
//        }
        binding.etAccountNumber.visibility = View.GONE

        //in case applied nomor rekening
//        if (binding.etAccountNumber.text.toString().trim().isEmpty()) {
//            Toast.makeText(this, "Silahkan isi nomor rekening/HP", Toast.LENGTH_SHORT).show()
//            return
//        }

//        if (binding.tvPaymentDate.text.toString() == "Pilih tanggal") {
//            Toast.makeText(this, "Silahkan pilih tanggal pembayaran", Toast.LENGTH_SHORT).show()
//            return
//        }

        // All validations passed, proceed with upload
        PopUpDialog.showConfirmDialog(
            context = this,
            title = "Apakah bukti yang dikirimkan sudah benar?",
            message = "Pastikan bukti yang dikirimkan valid",
            positiveText = "Ya",
            negativeText = "Tidak",
            onYesClicked = {
                uploadPaymentProof()
            }
        )
    }

    private fun uploadPaymentProof() {
        selectedImageUri?.let { uri ->
            // Convert URI to File
            val file = getFileFromUri(uri)
            file?.let {
                try {
                    // Create MultipartBody.Part from File
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val evidencePart = MultipartBody.Part.createFormData("evidence", file.name, requestFile)

                    // Create RequestBody for order ID and amount
                    val orderIdPart = orderId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    // Clean up the price string to get only the numeric value
                    val amountPart = productPrice.replace("Rp", "").replace(".", "").trim()
                        .toRequestBody("text/plain".toMediaTypeOrNull())

                    // Create the request object with the parts we need
                    val request = AddEvidenceMultipartRequest(
                        orderId = orderIdPart,
                        amount = amountPart,
                        evidence = evidencePart
                    )

                    // Log request details for debugging
                    Log.d(TAG, "Uploading payment proof - OrderID: $orderId, Amount: ${productPrice.replace("Rp", "").replace(".", "").trim()}")
                    Log.d(TAG, "File details - Name: ${file.name}, Size: ${file.length()} bytes, MIME: image/jpeg")

                    // Call the viewModel method
                    viewModel.uploadPaymentProof(request)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating upload request: ${e.message}", e)
                    Toast.makeText(this, "Gagal mengunggah foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = applicationContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

        Log.d("UploadEvidence", "URI: $uri")
        Log.d("UploadEvidence", "Detected MIME type: $mimeType, extension: $extension")

        // Ensure it's an image (either PNG, JPG, or JPEG)
        if (mimeType != "image/png" && mimeType != "image/jpeg" && mimeType != "image/jpg") {
            Log.e("UploadEvidence", "Invalid image MIME type: $mimeType. Only images are allowed.")
            Toast.makeText(applicationContext, "Only image files are allowed", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream == null) {
                Log.e("UploadEvidence", "Failed to open input stream from URI: $uri")
                return null
            }

            // Create a temporary file with the correct extension
            val tempFile = File.createTempFile("evidence_", ".$extension", cacheDir)
            Log.d("UploadEvidence", "Temp file created at: ${tempFile.absolutePath}")

            // Copy the content from inputStream to the temporary file
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
                inputStream.close()
            }

            // Verify if the file is a valid image
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap == null) {
                Log.e("UploadEvidence", "File is not a valid image!")
                tempFile.delete()
                return null
            } else {
                bitmap.recycle()  // Free memory
                Log.d("UploadEvidence", "Valid image detected.")
            }

            Log.d("UploadEvidence", "File copied successfully. Size: ${tempFile.length()} bytes")
            return tempFile
        } catch (e: Exception) {
            Log.e("UploadEvidence", "Error processing file: ${e.message}", e)
            Toast.makeText(applicationContext, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }


    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.tvPaymentDate.text = sdf.format(calendar.time)
            },
            year, month, day
        ).show()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "AddEvidenceActivity"
        private const val REQUEST_CODE_STORAGE_PERMISSION = 100

    }
}