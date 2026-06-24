package com.market.temues

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TemUESApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.STRIPE_KEY.isNotEmpty()) {
            PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_KEY)
        }
        crearCanalNotificaciones()
        guardarFcmTokenAlIniciarSesion()
    }

    private fun guardarFcmTokenAlIniciarSesion() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid ?: return@addAuthStateListener
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val atributos = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val canal = NotificationChannel(
                CANAL_MENSAJES_CHAT,
                "Mensajes de chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos mensajes"
                setSound(sonido, atributos)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    companion object {
        const val CANAL_MENSAJES_CHAT = "chat_messages_v2"
    }
}
