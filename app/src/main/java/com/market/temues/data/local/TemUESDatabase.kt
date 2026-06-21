package com.market.temues.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.local.entity.CarritoEntidad

@Database(entities = [CarritoEntidad::class], version = 1, exportSchema = false)
abstract class TemUESDatabase : RoomDatabase() {
    abstract fun carritoDao(): CarritoDao
}
