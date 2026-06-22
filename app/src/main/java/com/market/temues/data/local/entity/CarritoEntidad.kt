package com.market.temues.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carrito")
data class CarritoEntidad(
    @PrimaryKey val productoId: String,
    val nombreProducto: String,
    val precio: Double,
    val urlImagen: String,
    val cantidad: Int = 1,
    val stockMaximo: Int = 1,
    val vendedorId: String = "",
    val lugarEntrega: String = ""
)
