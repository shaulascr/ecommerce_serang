package com.alya.ecommerce_serang.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.ProductsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.FragmentHomeBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.HorizontalMarginItemDecoration
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.setLightStatusBar
import kotlinx.coroutines.launch

//@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var productAdapter: HorizontalProductAdapter? = null
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
    }

    private fun setupRecyclerView() {
        productAdapter = HorizontalProductAdapter(
            products = emptyList(),
            onClick = { product -> handleProductClick(product) }
        )

        binding.newProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HomeUiState.Loading -> {
                        binding.loading.root.isVisible = true
                        binding.error.root.isVisible = false
                        binding.home.isVisible = false
                    }
                    is HomeUiState.Success -> {
                        binding.loading.root.isVisible = false
                        binding.error.root.isVisible = false
                        binding.home.isVisible = true
                        productAdapter?.updateProducts(state.products)
                    }
                    is HomeUiState.Error -> {
                        binding.loading.root.isVisible = false
                        binding.error.root.isVisible = true
                        binding.home.isVisible = false
                        binding.error.errorMessage.text = state.message
                        binding.error.retryButton.setOnClickListener {
                            viewModel.retry()
                        }
                    }
                }
            }
        }
    }

    private fun initUi() {
        // For LightStatusBar
        setLightStatusBar()
        val banners = binding.banners
        banners.offscreenPageLimit = 1

        val nextItemVisiblePx = resources.getDimension(R.dimen.viewpager_next_item_visible)
        val currentItemHorizontalMarginPx =
            resources.getDimension(R.dimen.viewpager_current_item_horizontal_margin)
        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx

        banners.setPageTransformer { page, position ->
            page.translationX = -pageTranslationX * position
            page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
        }

        banners.addItemDecoration(
            HorizontalMarginItemDecoration(
                requireContext(),
                R.dimen.viewpager_current_item_horizontal_margin
            )
        )
    }


    private fun handleProductClick(product: ProductsItem) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
}