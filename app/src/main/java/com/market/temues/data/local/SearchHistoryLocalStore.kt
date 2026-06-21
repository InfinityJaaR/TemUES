package com.market.temues.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryLocalStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("temues_search_history", Context.MODE_PRIVATE)

    fun guardarBusqueda(texto: String) {
        val busqueda = texto.trim().lowercase()
        if (busqueda.isBlank()) return

        val historial = obtenerBusquedas().toMutableList()
        historial.remove(busqueda)
        historial.add(0, busqueda)
        preferences.edit()
            .putStringSet(KEY_BUSQUEDAS, historial.take(20).toSet())
            .apply()
    }

    fun obtenerBusquedas(): List<String> {
        return preferences.getStringSet(KEY_BUSQUEDAS, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }
    }

    private companion object {
        const val KEY_BUSQUEDAS = "busquedas"
    }
}
