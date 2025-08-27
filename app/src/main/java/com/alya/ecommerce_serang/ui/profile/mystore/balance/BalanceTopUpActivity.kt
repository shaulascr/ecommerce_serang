package com.alya.ecommerce_serang.ui.profile.mystore.balance

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.store.profile.Payment
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.utils.ImageUtils.compressImage
import com.alya.ecommerce_serang.utils.PopUpDialog
import com.alya.ecommerce_serang.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BalanceTopUpActivity : AppCompatActivity() {
    private lateinit var imgPreview: ImageView
    private lateinit var addPhotoTextView: TextView
    private lateinit var edtNominal: EditText
    private lateinit var spinnerPaymentMethod: Spinner
    private lateinit var edtTransactionDate: EditText
    private lateinit var datePickerIcon: ImageView
    private lateinit var layoutMBankingInstructions: View
    private lateinit var layoutATMInstructions: View
    private lateinit var btnSend: Button
    private lateinit var sessionManager: SessionManager

    private var selectedImageUri: Uri? = null
    private var paymentMethods: List<Payment> = emptyList()
    private var selectedPaymentId: Int = -1

    private val calendar = Calendar.getInstance()

    private val getImageContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                selectedImageUri = it

                // Compress the image before displaying it
                val compressedFile = compressImage(
                    context = this,
                    uri = it,
                    filename = "topup_img",
                    maxWidth = 1024,
                    maxHeight = 1024,
                    quality = 80
                )

                // Display the compressed image
                selectedImageUri = Uri.fromFile(compressedFile)
                imgPreview.setImageURI(Uri.fromFile(compressedFile))

                validateForm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_top_up)

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Initialize views
        imgPreview = findViewById(R.id.img_preview)
        addPhotoTextView = findViewById(R.id.tv_tambah_foto)
        edtNominal = findViewById(R.id.edt_nominal_topup)
        spinnerPaymentMethod = findViewById(R.id.spinner_metode_bayar)
        edtTransactionDate = findViewById(R.id.edt_tgl_transaksi)
        datePickerIcon = findViewById(R.id.img_date_picker)
        layoutMBankingInstructions = findViewById(R.id.layout_mbanking_instructions)
        layoutATMInstructions = findViewById(R.id.layout_atm_instructions)
        btnSend = findViewById(R.id.btn_send)

        // Setup header title
        val headerTitle = findViewById<TextView>(R.id.header_title)
        headerTitle.text = "Isi Ulang Saldo"

        // Setup back button
        val backButton = findViewById<ImageView>(R.id.header_left_icon)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Setup photo selection
        addPhotoTextView.setOnClickListener {
            openGallery()
        }

        imgPreview.setOnClickListener {
            openGallery()
        }

        // Setup date picker
        setupDatePicker()

        // Fetch payment methods
        fetchPaymentMethods()

        setupClickListeners("1234567890")

        // Setup submit button
        btnSend.setOnClickListener {
            submitForm()
        }

        // Validate form when any input changes
        edtNominal.doAfterTextChanged { validateForm() }
        edtTransactionDate.doAfterTextChanged { validateForm() }
        spinnerPaymentMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPaymentId = paymentMethods[position].id
                validateForm()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPaymentId = -1
                validateForm()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageContent.launch(intent)
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        edtTransactionDate.setOnClickListener {
            showDatePicker(dateSetListener)
        }

        datePickerIcon.setOnClickListener {
            showDatePicker(dateSetListener)
        }
    }

    private fun showDatePicker(dateSetListener: DatePickerDialog.OnDateSetListener) {
        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateInView() {
        val format = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(format, Locale.US)
        edtTransactionDate.setText(sdf.format(calendar.time))
    }

    private fun fetchPaymentMethods() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(sessionManager).getStoreData()
                if (response.isSuccessful && response.body() != null) {
                    val storeData = response.body()!!
                    paymentMethods = storeData.payment

                    setupPaymentMethodSpinner()
                } else {
                    Toast.makeText(
                        this@BalanceTopUpActivity,
                        "Gagal memuat metode pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BalanceTopUpActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupPaymentMethodSpinner() {
        if (paymentMethods.isEmpty()) {
            Toast.makeText(
                this,
                "Tidak ada metode pembayaran tersedia",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Debug payment methods
        for (payment in paymentMethods) {
            android.util.Log.d("BalanceTopUp", "Payment Option - ID: ${payment.id}, Bank: ${payment.bankName}, Number: ${payment.bankNum}")
        }

        val paymentOptions = paymentMethods.map { "${it.bankName} - ${it.bankNum}" }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerPaymentMethod.adapter = adapter
        spinnerPaymentMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPaymentId = paymentMethods[position].id
                android.util.Log.d("BalanceTopUp", "Selected payment ID: $selectedPaymentId")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPaymentId = -1
            }
        }
    }

    private fun validateForm() {
        val isNominalFilled = edtNominal.text.toString().trim().isNotEmpty()
        val isPaymentMethodSelected = selectedPaymentId != -1
        val isTransactionDateFilled = edtTransactionDate.text.toString().trim().isNotEmpty()
        val isImageSelected = selectedImageUri != null

        val valid = isNominalFilled && isPaymentMethodSelected && isTransactionDateFilled && isImageSelected
        btnSend.isEnabled = valid
        btnSend.setTextColor(
            if (valid) ContextCompat.getColor(this, R.color.white)
            else ContextCompat.getColor(this, R.color.black_300)
        )
        btnSend.setBackgroundResource(
            if (valid) R.drawable.bg_button_active
            else R.drawable.bg_button_disabled
        )
    }

    private fun submitForm() {
        // Prevent multiple clicks
        if (!btnSend.isEnabled) {
            return
        }

        // Validate inputs
        if (selectedImageUri == null) {
            Toast.makeText(this, "Mohon pilih foto bukti pembayaran", Toast.LENGTH_SHORT).show()
            return
        }

        val nominal = edtNominal.text.toString().trim()
        if (nominal.isEmpty()) {
            Toast.makeText(this, "Mohon isi nominal top up", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Validate the amount is a valid number
            val amountValue = nominal.replace("[^0-9]".toRegex(), "").toLong()
            if (amountValue <= 0) {
                Toast.makeText(this, "Nominal harus lebih dari 0", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Format nominal tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Mohon pilih metode pembayaran", Toast.LENGTH_SHORT).show()
            return
        }

        val transactionDate = edtTransactionDate.text.toString().trim()
        if (transactionDate.isEmpty()) {
            Toast.makeText(this, "Mohon pilih tanggal transaksi", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress indicator
        btnSend.text = "Mengirim..."
        btnSend.isEnabled = false

        // Proceed with the API call
        uploadTopUpData(nominal, selectedPaymentId.toString(), transactionDate)
    }

    private fun uploadTopUpData(amount: String, paymentInfoId: String, transactionDate: String) {
        lifecycleScope.launch {
            try {
                // Log the values being sent
                android.util.Log.d("BalanceTopUp", "Amount: $amount")
                android.util.Log.d("BalanceTopUp", "Payment ID: $paymentInfoId")
                android.util.Log.d("BalanceTopUp", "Transaction Date: $transactionDate")

                // Find the selected payment method to get bank name
                val selectedPayment = paymentMethods.find { it.id.toString() == paymentInfoId }
                if (selectedPayment == null) {
                    Toast.makeText(
                        this@BalanceTopUpActivity,
                        "Metode pembayaran tidak valid",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val bankName = selectedPayment.bankName
                val bankNum = selectedPayment.bankNum
                android.util.Log.d("BalanceTopUp", "Bank Name: $bankName")
                android.util.Log.d("BalanceTopUp", "Bank Number: $bankNum")

                // Get the actual file from URI
                val file = uriToFile(selectedImageUri!!)
                android.util.Log.d("BalanceTopUp", "File size: ${file.length()} bytes")
                android.util.Log.d("BalanceTopUp", "File name: ${file.name}")

                // Create multipart file with specific JPEG content type
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("topupimg", file.name, requestFile)

                // Create other request bodies - ensure proper formatting
                // Make sure amount has no commas, spaces or currency symbols
                val cleanedAmount = amount.replace("[^0-9]".toRegex(), "")
                val amountBody = cleanedAmount.toRequestBody("text/plain".toMediaTypeOrNull())
                val paymentInfoIdBody = paymentInfoId.toRequestBody("text/plain".toMediaTypeOrNull())
                val transactionDateBody = transactionDate.toRequestBody("text/plain".toMediaTypeOrNull())
                val bankNameBody = bankName.toRequestBody("text/plain".toMediaTypeOrNull())
                val bankNumBody = bankNum.toRequestBody("text/plain".toMediaTypeOrNull())

                // Make the API call
                val response = ApiConfig.getApiService(sessionManager).addBalanceTopUp(
                    imagePart,
                    amountBody,
                    paymentInfoIdBody,
                    transactionDateBody,
                    bankNameBody,
                    bankNumBody
                )

                if (response.isSuccessful) {
                    // Log the complete response
                    val responseBody = response.body()
                    android.util.Log.d("BalanceTopUp", "Success response: ${responseBody?.message}")

                    // Show the actual message from backend
                    val successMessage = "Top Up Berhasil"
                    Toast.makeText(
                        this@BalanceTopUpActivity,
                        successMessage,
                        Toast.LENGTH_LONG
                    ).show()

                    // Show a dialog with the success message
                    runOnUiThread {
                        PopUpDialog.showConfirmDialog(
                            context = this@BalanceTopUpActivity,
                            iconRes = R.drawable.checkmark__1_,
                            title = "Berhasil melakukan Top-Up"
                        )
                    }
                } else {
                    // Get more detailed error information
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("BalanceTopUp", "Error body: $errorBody")
                    android.util.Log.e("BalanceTopUp", "Error code: ${response.code()}")

                    // Try to parse the error body to extract the message
                    var errorMessage = "Gagal mengirim permintaan: ${response.message() ?: "Error ${response.code()}"}"
                    try {
                        val jsonObject = org.json.JSONObject(errorBody ?: "{}")
                        if (jsonObject.has("message")) {
                            errorMessage = jsonObject.getString("message")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BalanceTopUp", "Error parsing error body", e)
                    }

                    Toast.makeText(
                        this@BalanceTopUpActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()

                    // Show a dialog with the error message
                    runOnUiThread {

                        PopUpDialog.showConfirmDialog(
                            context = this@BalanceTopUpActivity,
                            iconRes = R.drawable.ic_cancel,
                            title = "Gagal melakukan Top-Up"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BalanceTopUp", "Exception: ${e.message}", e)
                Toast.makeText(
                    this@BalanceTopUpActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Reset button state
                btnSend.text = "Kirim"
                btnSend.isEnabled = true
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
        tempFile.deleteOnExit()

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Validate file isn't empty
        if (tempFile.length() == 0L) {
            throw IllegalStateException("File is empty")
        }

        return tempFile
    }

    private fun setupClickListeners(bankAccountNumber: String) {
        // Instructions clicks
        layoutMBankingInstructions.setOnClickListener {
            showInstructions("mBanking", bankAccountNumber)
        }

        layoutATMInstructions.setOnClickListener {
            showInstructions("ATM", bankAccountNumber)
        }
    }

    private fun showInstructions(type: String, bankAccountNumber: String) {
        // Implementasi tampilkan instruksi
        val instructions = when (type) {
            "mBanking" -> listOf(
                "1. Login ke aplikasi mobile banking",
                "2. Pilih menu Transfer",
                "3. Pilih menu Antar Rekening",
                "4. Masukkan nomor rekening tujuan: $bankAccountNumber",
                "5. Masukkan nominal saldo yang ingin diisi",
                "6. Konfirmasi dan selesaikan transfer"
            )
            "ATM" -> listOf(
                "1. Masukkan kartu ATM dan PIN",
                "2. Pilih menu Transfer",
                "3. Pilih menu Antar Rekening",
                "4. Masukkan kode bank dan nomor rekening tujuan: $bankAccountNumber",
                "5. Masukkan nominal saldo yang ingin diisi",
                "6. Konfirmasi dan selesaikan transfer"
            )
            else -> emptyList()
        }

        // Tampilkan instruksi dalam dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Petunjuk Transfer $type")
            .setItems(instructions.toTypedArray(), null)
            .setPositiveButton("Tutup", null)
            .create()
        dialog.show()
    }
}