package com.alya.ecommerce_serang.ui.profile.mystore.sells

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commit
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.SellsRepository
import com.alya.ecommerce_serang.databinding.ActivitySellsBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.SellsViewModel

class SellsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_INITIAL_STATUS = "extra_initial_status"
    }
    private lateinit var binding: ActivitySellsBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: SellsViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val sellsRepository = SellsRepository(apiService)
            SellsViewModel(sellsRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellsBinding.inflate(layoutInflater)
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

        Log.d("SellsActivity", "SellsActivity started")

        setupHeader()

        if (savedInstanceState == null) {
            showSellsFragment()
        }
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Penjualan Saya"

        binding.header.headerLeftIcon.setOnClickListener {
            onBackPressed()
            finish()
        }

//        binding.edtSearch.doAfterTextChanged {
//            val q = it?.toString()?.trim().orEmpty()
//            (supportFragmentManager.findFragmentById(R.id.fragment_container_sells) as? SellsFragment)
//                ?.onSearchQueryChanged(q)
//        }
    }

    private fun showSellsFragment() {
        val initialStatus = intent.getStringExtra(EXTRA_INITIAL_STATUS)
        supportFragmentManager.commit {
            replace(R.id.fragment_container_sells, SellsFragment.newInstance(initialStatus))
        }
    }
}