package com.alya.ecommerce_serang.ui.profile.mystore.review

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ReviewRepository
import com.alya.ecommerce_serang.databinding.FragmentReviewListBinding
import com.alya.ecommerce_serang.ui.order.address.ViewState
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager
import com.alya.ecommerce_serang.utils.viewmodel.ProductViewModel
import com.alya.ecommerce_serang.utils.viewmodel.ReviewViewModel

class ReviewListFragment : Fragment() {

    private var _binding: FragmentReviewListBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private lateinit var reviewAdapter: ReviewAdapter
    private val viewModel: ReviewViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(SessionManager(requireContext()))
            ReviewViewModel(ReviewRepository(apiService))
        }
    }

    private var score: String = "all"

    companion object {
        private const val ARG_SCORE = "score"

        fun newInstance(score: String): ReviewListFragment = ReviewListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SCORE, score)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
        score = arguments?.getString(ARG_SCORE) ?: "all"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReviewListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reviewAdapter = ReviewAdapter(viewModel)
        binding.rvReview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }

        observeReviewList()
        fetchReviewByScore(score)
    }

    private fun fetchReviewByScore(score: String) {
        val normalizedScore = when (score) {
            "all" -> "all"
            else -> {
                val scoreValue = score.toDoubleOrNull() ?: 0.0
                when {
                    scoreValue > 4.5 -> "5"
                    scoreValue > 3.5 -> "4"
                    scoreValue > 2.5 -> "3"
                    scoreValue > 1.5 -> "2"
                    else -> "1"
                }
            }
        }
        viewModel.getReview(normalizedScore)
    }

    private fun observeReviewList() {
        viewModel.review.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ViewState.Success -> {
                    val data = result.data.orEmpty().sortedByDescending { it.reviewDate }
                    binding.progressBar.visibility = View.GONE

                    if (data.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvReview.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvReview.visibility = View.VISIBLE
                        reviewAdapter.submitList(data)
                    }
                }

                is ViewState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is ViewState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}