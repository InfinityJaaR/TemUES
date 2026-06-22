package com.market.temues.ui.seller

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.market.temues.databinding.FragmentSellerCatalogBinding
import com.market.temues.model.Product
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SellerCatalogFragment : Fragment() {

    private var _binding: FragmentSellerCatalogBinding? = null
    private val binding get() = _binding!!

    private val modelo: SellerCatalogViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBotonFlotante()
        observarEstado()
    }

    private fun configurarBotonFlotante() {
        binding.fabAddProduct.setOnClickListener {
            val accion = SellerCatalogFragmentDirections.actionSellerCatalogToAddEditProduct("")
            findNavController().navigate(accion)
        }
    }

    private fun observarEstado() {
        modelo.estadoUI.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.lottieLoading.isVisible = estado is EstadoCatalogoUI.Cargando
            binding.layoutEmpty.isVisible = estado is EstadoCatalogoUI.Vacio

            when (estado) {
                is EstadoCatalogoUI.Exito -> {
                    binding.composeView.isVisible = true
                    binding.composeView.setContent {
                        SellerCatalogContent(
                            productos = estado.productos,
                            onEdit = { producto ->
                                val accion = SellerCatalogFragmentDirections
                                    .actionSellerCatalogToAddEditProduct(producto.id)
                                findNavController().navigate(accion)
                            },
                            onDelete = { modelo.eliminarProducto(it.id) },
                            onToggleStatus = { modelo.cambiarEstado(it.id, it.status) }
                        )
                    }
                }
                is EstadoCatalogoUI.Error -> {
                    binding.composeView.isVisible = false
                }
                else -> binding.composeView.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
private fun SellerCatalogContent(
    productos: List<Product>,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onToggleStatus: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(productos, key = { it.id }) { producto ->
            SwipeableProductItem(
                producto = producto,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleStatus = onToggleStatus
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableProductItem(
    producto: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onToggleStatus: (Product) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentProducto = rememberUpdatedState(producto)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteConfirm = true
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleStatus(currentProducto.value)
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                scope.launch { dismissState.reset() }
            },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de eliminar \"${producto.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(producto)
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
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
            val isDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor = if (isDelete) Color(0xFFF44336) else Color(0xFF4CAF50)
            val label =
                if (isDelete) "Eliminar"
                else if (producto.status == "activo") "Vendido"
                else "Reactivar"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = bgColor, shape = RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        ProductCard(
            producto = producto,
            onEdit = onEdit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    producto: Product,
    onEdit: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(producto) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(
                    url = producto.images.firstOrNull(),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", producto.price)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (producto.status == "activo") "Activo" else "Vendido",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun ProductImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val appContext = LocalContext.current.applicationContext
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(url) {
        val target = object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                bitmap.value = resource
            }
            override fun onLoadCleared(placeholder: Drawable?) {
                bitmap.value = null
            }
        }
        Glide.with(appContext)
            .asBitmap()
            .load(url)
            .into(target)
        onDispose { Glide.with(appContext).clear(target) }
    }

    bitmap.value?.asImageBitmap()?.let { imageBitmap ->
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
