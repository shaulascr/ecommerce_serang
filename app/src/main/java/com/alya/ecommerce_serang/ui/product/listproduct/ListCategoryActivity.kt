package com.alya.ecommerce_serang.ui.product.listproduct

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.ActivityListCategoryBinding
import com.alya.ecommerce_serang.ui.product.category.CategoryProductsActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.HomeViewModel

class ListCategoryActivity : AppCompatActivity() {
    companion object {
        private val TAG = "ListCategoryActivity"
    }
    private lateinit var binding: ActivityListCategoryBinding
    private lateinit var sessionManager: SessionManager
    private var categoryAdapter: ListCategoryAdapter? = null

    private val viewModel: HomeViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val repository = ProductRepository(apiService)
            HomeViewModel(repository)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

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

        setupToolbar()
        setupObserver()
        viewModel.loadCategory()
        setupRecyclerView()

    }

    private fun setupToolbar(){
        binding.header.headerLeftIcon.setOnClickListener{
            finish()
        }
        binding.header.headerTitle.text = "Kategori Produk"
    }

    private fun setupRecyclerView(){
        categoryAdapter = ListCategoryAdapter(
            categories = emptyList(),
            onClick = { category ->  handleClickOnCategory(category) }
        )

        binding.rvListCategories.apply {
            adapter = categoryAdapter
            layoutManager = GridLayoutManager(
                context,
                3, // 3 columns
                RecyclerView.VERTICAL,
                false
            )
        }
    }

    private  fun setupObserver(){
        viewModel.category.observe(this) { category ->
            Log.d("ListCategoryActivity", "Received categories: ${category.size}")
            updateCategories(category)
        }
    }

    private fun updateCategories(category: List<CategoryItem>){
        if (category.isEmpty()) {
            binding.rvListCategories.visibility = View.GONE
        } else {
            binding.rvListCategories.visibility = View.VISIBLE
            categoryAdapter?.updateData(category)
        }
    }

    private fun handleClickOnCategory(category: CategoryItem){
        val intent = Intent(this, CategoryProductsActivity::class.java)
        intent.putExtra(CategoryProductsActivity.EXTRA_CATEGORY, category)
        startActivity(intent)
    }
}