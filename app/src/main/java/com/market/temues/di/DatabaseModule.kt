package com.market.temues.di

import android.content.Context
import androidx.room.Room
import com.market.temues.data.local.TemUESDatabase
import com.market.temues.data.local.dao.CarritoDao
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
    fun proveerBaseDeDatos(@ApplicationContext contexto: Context): TemUESDatabase =
        Room.databaseBuilder(contexto, TemUESDatabase::class.java, "temues_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun proveerCarritoDao(baseDeDatos: TemUESDatabase): CarritoDao =
        baseDeDatos.carritoDao()
}
