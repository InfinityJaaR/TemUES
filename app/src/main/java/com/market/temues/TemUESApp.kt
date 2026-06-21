package com.market.temues

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.service.TemUESMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TemUESApp : Application() {

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
        configurarEscuchaNotificaciones()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_MENSAJES_CHAT,
                "Mensajes de chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos mensajes"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    private fun configurarEscuchaNotificaciones() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                TemUESMessagingService.iniciarEscucha(applicationContext, uid)
            } else {
                TemUESMessagingService.detenerEscucha()
            }
        }
    }

    companion object {
        const val CANAL_MENSAJES_CHAT = "chat_messages"
    }
}
