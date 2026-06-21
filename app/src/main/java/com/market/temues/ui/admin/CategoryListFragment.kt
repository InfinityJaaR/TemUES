package com.market.temues.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.market.temues.R
import com.market.temues.databinding.FragmentCategoryListBinding
import com.market.temues.model.Category
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
            findNavController().navigate(R.id.adminCreateCategoryFragment)
        }

        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is CategoryListUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.composeView.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }
                is CategoryListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    if (state.categories.isEmpty()) {
                        binding.composeView.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.composeView.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                        binding.composeView.setContent {
                            CategoryListContent(
                                categories = state.categories,
                                onEdit = { editCategory(it) },
                                onDelete = { viewModel.deleteCategory(it.id) }
                            )
                        }
                    }
                }
                is CategoryListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.composeView.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
            }
        }
    }

    private fun editCategory(category: Category) {
        val bundle = Bundle().apply {
            putString("categoryId", category.id)
        }
        findNavController().navigate(R.id.adminCreateCategoryFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
private fun CategoryListContent(
    categories: List<Category>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            SwipeableCategoryItem(
                category = category,
                onEdit = onEdit,
                onDelete = onDelete
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else {
                false
            }
        }
    )

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                scope.launch { dismissState.reset() }
            },
            title = { Text("Eliminar categoría") },
            text = { Text("¿Estás seguro de eliminar la categoría \"${category.name}\"?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(category)
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { dismissState.reset() }
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFF44336),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Eliminar",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(category) },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFDFDFD)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2A4A)
                    )
                    Text(
                        text = "Orden ${category.order}",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
                Text(
                    text = "\u203A",
                    fontSize = 22.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}
