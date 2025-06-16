package com.alya.ecommerce_serang.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.alya.ecommerce_serang.data.api.dto.ProductsItem
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ProductRepository
import com.alya.ecommerce_serang.databinding.FragmentSearchHomeBinding
import com.alya.ecommerce_serang.ui.product.DetailProductActivity
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class SearchHomeFragment : Fragment() {
    private var _binding: FragmentSearchHomeBinding? = null
    private val binding get() = _binding!!
    private var searchResultsAdapter: SearchResultsAdapter? = null
    private lateinit var sessionManager: SessionManager
    private val args: SearchHomeFragmentArgs by navArgs()

    private val viewModel: SearchHomeViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val productRepository = ProductRepository(apiService)
            SearchHomeViewModel(productRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupSearchResultsRecyclerView()
        observeData()

        // Perform search with the query passed from HomeFragment
        args.query?.let { query ->
            // Wait until layout is done, then set query text
            binding.searchView.post {
                binding.searchView.setQuery(query, false) // sets "food" as text, doesn't submit
            }

            viewModel.searchProducts(query)
        }
    }

    private fun setupUI() {
        // Setup back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup search view
        binding.searchView.apply {
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        if (it.isNotEmpty()) {
                            viewModel.searchProducts(it)
                            hideKeyboard()
                        }
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        if (it.isEmpty()) {
                            // Clear the search results if user clears the input
                            searchResultsAdapter?.submitList(emptyList())
                            binding.noResultsText.isVisible = false
                            return true
                        }

                        // Optional: do real-time search
                        if (it.length >= 2) {
                            viewModel.searchProducts(it)
                        }
                    }
                    return true
                }
            })

            // Request focus and show keyboard
            if (args.query.isNullOrEmpty()) {
                requestFocus()
                postDelayed({
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(findFocus(), InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            }
        }
    }

    private fun setupSearchResultsRecyclerView() {

        binding.searchResultsRecyclerView.apply {
            adapter = searchResultsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeData() {
        viewModel.searchResults.observe(viewLifecycleOwner) { products ->

            if (!products.isNullOrEmpty()){
                viewModel.storeDetail(products)
            }

            searchResultsAdapter?.submitList(products)
            binding.noResultsText.isVisible = products.isEmpty() && !viewModel.isSearching.value!!
            binding.searchResultsRecyclerView.isVisible = products.isNotEmpty()

        }

        viewModel.storeDetail.observe(viewLifecycleOwner) {storeMap ->
            val products = viewModel.searchResults.value.orEmpty()
            if (products.isNotEmpty()){
                searchResultsAdapter = SearchResultsAdapter(
                    onItemClick = {product -> navigateToProductDetail(product) },
                    storeMap = storeMap
                )
                binding.searchResultsRecyclerView.adapter = searchResultsAdapter
                searchResultsAdapter?.submitList(products)
            }
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.isVisible = isSearching
        }
    }

    private fun navigateToProductDetail(product: ProductsItem) {
        val intent = Intent(requireContext(), DetailProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id)
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.searchView.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        searchResultsAdapter = null
        _binding = null
    }
}