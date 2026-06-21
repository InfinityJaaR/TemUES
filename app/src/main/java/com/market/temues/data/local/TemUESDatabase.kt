package com.market.temues.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.market.temues.data.local.dao.FavoriteDao
import com.market.temues.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TemUESDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}