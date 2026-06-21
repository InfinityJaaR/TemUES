package com.market.temues.di

import android.content.Context
import androidx.room.Room
import com.market.temues.data.local.TemUESDatabase
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.local.dao.FavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TemUESDatabase =
        Room.databaseBuilder(context, TemUESDatabase::class.java, "temues_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCarritoDao(db: TemUESDatabase): CarritoDao = db.carritoDao()

    @Provides
    fun provideFavoriteDao(db: TemUESDatabase): FavoriteDao = db.favoriteDao()
}
