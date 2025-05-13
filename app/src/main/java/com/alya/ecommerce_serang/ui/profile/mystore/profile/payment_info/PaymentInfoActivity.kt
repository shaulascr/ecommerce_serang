package com.alya.ecommerce_serang.ui.profile.mystore.profile.payment_info

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.dto.PaymentInfo
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.PaymentInfoRepository
import com.alya.ecommerce_serang.databinding.ActivityPaymentInfoBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.UriToFileConverter
import com.alya.ecommerce_serang.utils.viewmodel.PaymentInfoViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File

class PaymentInfoActivity : AppCompatActivity() {
    private val TAG = "PaymentInfoActivity"
    private lateinit var binding: ActivityPaymentInfoBinding
    private lateinit var adapter: PaymentInfoAdapter
    private lateinit var sessionManager: SessionManager
    private var selectedQrisImageUri: Uri? = null
    private var selectedQrisImageFile: File? = null

    // Store form data between dialog reopenings
    private var savedBankName: String = ""
    private var savedBankNumber: String = ""
    private var savedAccountName: String = ""

    private val viewModel: PaymentInfoViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val repository = PaymentInfoRepository(apiService)
            PaymentInfoViewModel(repository)
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                Log.d(TAG, "Selected image URI: $uri")
                selectedQrisImageUri = it

                // Convert URI to File
                selectedQrisImageFile = UriToFileConverter.uriToFile(it, this)

                if (selectedQrisImageFile == null) {
                    Log.e(TAG, "Failed to convert URI to file")
                    showSnackbar("Failed to process image. Please try another image.")
                    return@let
                }

                Log.d(TAG, "Converted to file: ${selectedQrisImageFile?.absolutePath}, size: ${selectedQrisImageFile?.length()} bytes")

                // Check if file exists and has content
                if (!selectedQrisImageFile!!.exists() || selectedQrisImageFile!!.length() == 0L) {
                    Log.e(TAG, "File doesn't exist or is empty: ${selectedQrisImageFile?.absolutePath}")
                    showSnackbar("Failed to process image. Please try another image.")
                    selectedQrisImageFile = null
                    return@let
                }

                showAddPaymentDialog(true) // Reopen dialog with selected image
            } catch (e: Exception) {
                Log.e(TAG, "Error processing selected image", e)
                showSnackbar("Error processing image: ${e.message}")
                selectedQrisImageUri = null
                selectedQrisImageFile = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Configure header
        binding.header.headerTitle.text = "Atur Metode Pembayaran"

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        setupObservers()

        binding.btnAddPayment.setOnClickListener {
            // Clear saved values when opening a new dialog
            savedBankName = ""
            savedBankNumber = ""
            savedAccountName = ""
            selectedQrisImageUri = null
            selectedQrisImageFile = null
            showAddPaymentDialog(false)
        }

        // Load payment info
        viewModel.getPaymentInfo()
    }

    private fun setupRecyclerView() {
        adapter = PaymentInfoAdapter(
            onDeleteClick = { paymentMethod ->
                showDeleteConfirmationDialog(paymentMethod)
            }
        )
        binding.rvPaymentInfo.layoutManager = LinearLayoutManager(this)
        binding.rvPaymentInfo.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.paymentInfos.observe(this) { paymentInfo ->
            binding.progressBar.visibility = View.GONE

            if (paymentInfo.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPaymentInfo.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPaymentInfo.visibility = View.VISIBLE
                adapter.submitList(paymentInfo)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddPayment.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showSnackbar(errorMessage)
                Log.e(TAG, "Error: $errorMessage")
            }
        }

        viewModel.addPaymentSuccess.observe(this) { success ->
            if (success) {
                showSnackbar("Metode pembayaran berhasil ditambahkan")
                setResult(Activity.RESULT_OK)
            }
        }

        viewModel.deletePaymentSuccess.observe(this) { success ->
            if (success) {
                showSnackbar("Metode pembayaran berhasil dihapus")
                setResult(Activity.RESULT_OK)
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showAddPaymentDialog(isReopened: Boolean) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_payment_info, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        // Get references to views in the dialog
        val btnAddQris = dialogView.findViewById<Button>(R.id.btn_add_qris)
        val bankNameEditText = dialogView.findViewById<EditText>(R.id.edt_bank_name)
        val bankNumberEditText = dialogView.findViewById<EditText>(R.id.edt_bank_number)
        val accountNameEditText = dialogView.findViewById<EditText>(R.id.edt_account_name)
        val qrisPreview = dialogView.findViewById<ImageView>(R.id.iv_qris_preview)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        // When reopening, restore the previously entered values
        if (isReopened) {
            bankNameEditText.setText(savedBankName)
            bankNumberEditText.setText(savedBankNumber)
            accountNameEditText.setText(savedAccountName)

            if (selectedQrisImageUri != null) {
                Log.d(TAG, "Showing selected QRIS image: $selectedQrisImageUri")
                qrisPreview.setImageURI(selectedQrisImageUri)
                qrisPreview.visibility = View.VISIBLE
                showSnackbar("Gambar QRIS berhasil dipilih")
            }
        }

        btnAddQris.setOnClickListener {
            // Save the current values before dismissing
            savedBankName = bankNameEditText.text.toString().trim()
            savedBankNumber = bankNumberEditText.text.toString().trim()
            savedAccountName = accountNameEditText.text.toString().trim()

            getContent.launch("image/*")
            dialog.dismiss() // Dismiss the current dialog as we'll reopen it
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val bankName = bankNameEditText.text.toString().trim()
            val bankNumber = bankNumberEditText.text.toString().trim()
            val accountName = accountNameEditText.text.toString().trim()

            // Validation
            if (bankName.isEmpty()) {
                showSnackbar("Nama bank tidak boleh kosong")
                return@setOnClickListener
            }

            if (bankNumber.isEmpty()) {
                showSnackbar("Nomor rekening tidak boleh kosong")
                return@setOnClickListener
            }

            if (accountName.isEmpty()) {
                showSnackbar("Nama pemilik rekening tidak boleh kosong")
                return@setOnClickListener
            }

            if (bankNumber.any { !it.isDigit() }) {
                showSnackbar("Nomor rekening hanya boleh berisi angka")
                return@setOnClickListener
            }

            // Log the data being sent
            Log.d(TAG, "====== SENDING PAYMENT METHOD DATA ======")
            Log.d(TAG, "Bank Name: $bankName")
            Log.d(TAG, "Bank Number: $bankNumber")
            Log.d(TAG, "Account Name: $accountName")
            if (selectedQrisImageFile != null) {
                Log.d(TAG, "QRIS file path: ${selectedQrisImageFile?.absolutePath}")
                Log.d(TAG, "QRIS file exists: ${selectedQrisImageFile?.exists()}")
                Log.d(TAG, "QRIS file size: ${selectedQrisImageFile?.length()} bytes")
            } else {
                Log.d(TAG, "No QRIS file selected")
            }

            // Temporarily disable the save button
            btnSave.isEnabled = false
            btnSave.text = "Menyimpan..."

            // Add payment info
            viewModel.addPaymentInfo(
                bankName = bankName,
                bankNumber = bankNumber,
                accountName = accountName,
                qrisImageUri = selectedQrisImageUri,
                qrisImageFile = selectedQrisImageFile
            )

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(paymentInfo: PaymentInfo) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Metode Pembayaran")
            .setMessage("Apakah Anda yakin ingin menghapus metode pembayaran ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deletePaymentInfo(paymentInfo.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}