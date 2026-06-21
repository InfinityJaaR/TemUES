package com.market.temues.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.FragmentAdminCreateCategoryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminCreateCategoryFragment : Fragment() {

    private var _binding: FragmentAdminCreateCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminCreateCategoryViewModel by viewModels()

    private var categoryId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminCreateCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryId = arguments?.getString("categoryId")

        if (categoryId != null) {
            viewModel.loadCategory(categoryId!!)
        }

        viewModel.categoryName.asLiveData().observe(viewLifecycleOwner) { name ->
            if (binding.etName.text.toString() != name) {
                binding.etName.setText(name)
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString() ?: ""
            viewModel.saveCategory(name, categoryId)
        }

        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateCategoryUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.tvError.visibility = View.GONE
                }
                is CreateCategoryUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                    binding.tvError.visibility = View.GONE
                }
                is CreateCategoryUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    val message = if (state.isUpdate) R.string.admin_category_updated
                    else R.string.admin_category_saved
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                findNavController().navigateUp()
                            }
                        })
                        .show()
                }
                is CreateCategoryUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
