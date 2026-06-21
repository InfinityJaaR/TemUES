package com.market.temues.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.market.temues.data.local.entity.CarritoEntidad
import kotlinx.coroutines.flow.Flow

@Dao
interface CarritoDao {

    @Query("SELECT * FROM carrito")
    fun obtenerTodos(): Flow<List<CarritoEntidad>>

    @Query("SELECT * FROM carrito WHERE productoId = :productoId")
    suspend fun obtenerArticulo(productoId: String): CarritoEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(articulo: CarritoEntidad)

    @Query("DELETE FROM carrito WHERE productoId = :productoId")
    suspend fun eliminar(productoId: String)

    @Query("DELETE FROM carrito")
    suspend fun limpiarTodo()

    @Query("SELECT SUM(precio * cantidad) FROM carrito")
    fun obtenerTotal(): Flow<Double?>
}
