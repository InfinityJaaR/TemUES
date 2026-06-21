package com.market.temues.data.local.dao

import androidx.room.*
import com.market.temues.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAll(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND productId = :productId)")
    fun isFavorite(userId: String, productId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND productId = :productId)")
    suspend fun isFavoriteSync(userId: String, productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: String, productId: String)

    @Query("SELECT * FROM favorites WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsynced(userId: String): List<FavoriteEntity>

    @Query("UPDATE favorites SET isSynced = 1 WHERE userId = :userId AND productId = :productId")
    suspend fun markAsSynced(userId: String, productId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}