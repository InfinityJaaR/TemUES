package com.market.temues.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.market.temues.R
import com.market.temues.databinding.FragmentAdminDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminDashboardUiState.Loading -> showLoading()
                is AdminDashboardUiState.Success -> showDashboard(state)
                is AdminDashboardUiState.Error -> showError(state.message)
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResumen.visibility = View.GONE
        binding.cardCategories.visibility = View.GONE
        binding.cardTrends.visibility = View.GONE
        binding.cardRecent.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun showDashboard(state: AdminDashboardUiState.Success) {
        binding.progressBar.visibility = View.GONE
        binding.cardResumen.visibility = View.VISIBLE
        binding.cardCategories.visibility = View.VISIBLE
        binding.cardTrends.visibility = View.VISIBLE
        binding.cardRecent.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        binding.tvTotalProducts.text = state.totalProducts.toString()

        val orange = ContextCompat.getColor(requireContext(), R.color.temues_orange)
        binding.layoutCategoryStats.removeAllViews()
        for (cat in state.productsByCategory) {
            val row = layoutInflater.inflate(R.layout.item_stat_row, binding.layoutCategoryStats, false) as LinearLayout
            row.findViewById<TextView>(android.R.id.text1).text = cat.categoryName
            row.findViewById<TextView>(android.R.id.text2).apply {
                text = "${cat.count} (${"%.1f".format(cat.percentage)}%)"
                setTextColor(orange)
            }
            binding.layoutCategoryStats.addView(row)
        }

        val green = ContextCompat.getColor(requireContext(), R.color.temues_green)
        val yellow = ContextCompat.getColor(requireContext(), R.color.temues_yellow)
        val red = ContextCompat.getColor(requireContext(), R.color.temues_red)
        binding.layoutTrends.removeAllViews()
        for (trend in state.categoryTrends) {
            val row = layoutInflater.inflate(R.layout.item_stat_row, binding.layoutTrends, false) as LinearLayout
            val (trendIcon, trendColor) = when (trend.trend) {
                "up" -> "\u2191" to green
                "down" -> "\u2193" to red
                else -> "\u2192" to yellow
            }
            row.findViewById<TextView>(android.R.id.text1).apply {
                text = trend.categoryName
                setTextColor(trendColor)
            }
            row.findViewById<TextView>(android.R.id.text2).apply {
                text = "$trendIcon ${trend.productCount}"
                setTextColor(trendColor)
            }
            binding.layoutTrends.addView(row)
        }

        binding.layoutRecentProducts.removeAllViews()
        for (product in state.recentProducts) {
            val row = layoutInflater.inflate(R.layout.item_stat_row, binding.layoutRecentProducts, false) as LinearLayout
            row.findViewById<TextView>(android.R.id.text1).text = product.name
            row.findViewById<TextView>(android.R.id.text2).apply {
                text = "$${"%.2f".format(product.price)}"
                setTextColor(orange)
            }
            binding.layoutRecentProducts.addView(row)
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.cardResumen.visibility = View.GONE
        binding.cardCategories.visibility = View.GONE
        binding.cardTrends.visibility = View.GONE
        binding.cardRecent.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
