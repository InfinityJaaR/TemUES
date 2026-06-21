package com.market.temues.model

data class Orden(
    val id: String = "",
    val usuarioId: String = "",
    val articulos: List<ArticuloOrden> = emptyList(),
    val total: Double = 0.0,
    val metodoPago: String = "",
    val estado: String = "pendiente",
    val codigo: String = "",
    val vendedorId: String = "",
    val lugarEntrega: String = "",
    val creadoEn: Long = System.currentTimeMillis()
)

data class ArticuloOrden(
    val productoId: String = "",
    val nombreProducto: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val urlImagen: String = ""
)
