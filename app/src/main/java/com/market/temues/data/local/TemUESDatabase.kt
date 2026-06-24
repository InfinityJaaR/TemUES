package com.market.temues.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.local.dao.FavoriteDao
import com.market.temues.data.local.entity.CarritoEntidad
import com.market.temues.data.local.entity.FavoriteEntity

@Database(
    entities = [CarritoEntidad::class, FavoriteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TemUESDatabase : RoomDatabase() {
    abstract fun carritoDao(): CarritoDao
    abstract fun favoriteDao(): FavoriteDao
}
