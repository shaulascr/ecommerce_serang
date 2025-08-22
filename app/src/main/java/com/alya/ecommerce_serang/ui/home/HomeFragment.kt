package com.alya.ecommerce_serang.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.dto.CategoryItem
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.response.customer.product.StoreItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.FragmentHomeBinding
import com.alya.ecommerce_serang.ui.cart.CartActivity
import com.alya.ecommerce_serang.ui.notif.NotificationActivity
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.ui.product.category.CategoryProductsActivity
import com.alya.ecommerce_serang.ui.product.listproduct.ListCategoryActivity
import com.alya.ecommerce_serang.ui.product.listproduct.ListProductActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.setLightStatusBar
import com.alya.ecommerce_serang.utils.viewmodel.HomeUiState
import com.alya.ecommerce_serang.utils.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

//@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var productAdapter: HorizontalProductAdapter? = null
    private var categoryAdapter: HomeCategoryAdapter? = null
    private lateinit var sessionManager: SessionManager
    private val viewModel: HomeViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            HomeViewModel(productRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        initUi()
        setupRecyclerView()
        observeData()
        setupSearchView()

    }

    private fun setupRecyclerView() {
        categoryAdapter = HomeCategoryAdapter(
            categories = emptyList(),
            onClick = { category ->  handleCategoryProduct(category)}
        )

        binding.newProducts.apply {
            adapter = productAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }

        binding.categories.apply {
            adapter = categoryAdapter
            layoutManager = GridLayoutManager(
                context,
                3, // 3 columns
                RecyclerView.VERTICAL, // vertical layout
                false
            )
        }

        binding.productshowAll.setOnClickListener {
            val intent = Intent(requireContext(), ListProductActivity::class.java)
            startActivity(intent)
        }

        binding.categoryShowAll.setOnClickListener {
            val intent = Intent(requireContext(), ListCategoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearchView() {
        binding.searchContainer.search.apply {
            // Make it non-editable so it acts like a button
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true

            setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToSearchHomeFragment(null)
                )
            }
        }

        // Setup cart and notification buttons
        binding.searchContainer.btnCart.setOnClickListener {
            // Navigate to cart
            val intent = Intent(requireContext(), CartActivity::class.java)
            startActivity(intent)
        }

        binding.searchContainer.btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            binding.loadingAll.visibility = View.VISIBLE
                            binding.error.root.isVisible = false
                            binding.home.isVisible = false
                        }
                        is HomeUiState.Success -> {
                            binding.loadingAll.visibility = View.GONE
                            binding.error.root.isVisible = false
                            binding.home.isVisible = true
                            val products = state.products
                            viewModel.loadStoresForProducts(products) // << add this here

                            productAdapter?.updateLimitedProducts(products)
                        }
                        is HomeUiState.Error -> {
                            binding.loadingAll.visibility = View.GONE
                            binding.error.root.isVisible = true
                            binding.home.isVisible = false
//                            binding.error.errorMessage.text = state.message
                            Log.e("HomeFragment", "Error load data: ${state.message}")
                            Toast.makeText(requireContext(), "Terjadi kendala. Muat ulang halaman", Toast.LENGTH_SHORT) .show()
                            binding.error.retryButton.setOnClickListener {
                                viewModel.retry()
                            }
                        }
                    }
                }

            }

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    Log.d("Categories", "Updated Categories: $categories")
                    categoryAdapter?.updateLimitedCategory(categories)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storeMap.collect { storeMap ->
                    val products = (viewModel.uiState.value as? HomeUiState.Success)?.products.orEmpty()
                    if (products.isNotEmpty()) {
                        updateProducts(products, storeMap)
                    }
                }
            }
        }
    }

    private fun updateProducts(products: List<ProductsItem>, storeMap: Map<Int, StoreItem>) {
        if (products.isEmpty()) {
            Log.d("HomeFragment", "Product list is empty, hiding RecyclerView")
            binding.newProducts.visibility = View.VISIBLE
        } else {
            Log.d("HomeFragment", "Displaying product list in RecyclerView")
            binding.newProducts.visibility = View.VISIBLE  // <-- Fix here
            productAdapter = HorizontalProductAdapter(products, onClick = { product ->
                handleProductClick(product)
            }, storeMap = storeMap)
            binding.newProducts.adapter = productAdapter
            productAdapter?.updateProducts(products)
        }
    }

    private fun initUi() {
        // For LightStatusBar
        setLightStatusBar()
    }

    private fun handleProductClick(product: ProductsItem) {
        val intent = Intent(requireContext(), DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id) // Pass product ID
        startActivity(intent)
    }

    private fun handleCategoryProduct(category: CategoryItem) {
        // Navigate to CategoryProductsActivity when category is clicked
        val intent = Intent(requireContext(), CategoryProductsActivity::class.java)
        intent.putExtra(CategoryProductsActivity.EXTRA_CATEGORY, category)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        productAdapter = null
        categoryAdapter = null
        _binding = null
    }
}