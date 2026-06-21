package com.market.temues.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.market.temues.R
import com.market.temues.databinding.FragmentCategoryListBinding
import com.market.temues.model.Category
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddCategory.setOnClickListener {
            val bundle = Bundle()
            findNavController().navigate(R.id.adminCreateCategoryFragment, bundle)
        }

        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is CategoryListUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutCategories.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }
                is CategoryListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    if (state.categories.isEmpty()) {
                        binding.layoutCategories.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutCategories.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                        showCategories(state.categories)
                    }
                }
                is CategoryListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutCategories.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
            }
        }
    }

    private fun showCategories(categories: List<Category>) {
        binding.layoutCategories.removeAllViews()
        for (category in categories) {
            val card = layoutInflater.inflate(R.layout.item_category_card, binding.layoutCategories, false) as MaterialCardView
            card.findViewById<TextView>(android.R.id.text1).text = category.name
            card.findViewById<TextView>(android.R.id.text2).text = "Orden ${category.order}"
            card.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("categoryId", category.id)
                findNavController().navigate(R.id.adminCreateCategoryFragment, bundle)
            }
            binding.layoutCategories.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
