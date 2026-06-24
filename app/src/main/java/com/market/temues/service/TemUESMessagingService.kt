package com.market.temues.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.market.temues.MainActivity
import com.market.temues.R
import com.market.temues.TemUESApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TemUESMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val tipo = remoteMessage.data["type"] ?: return
        if (tipo == "chat_message") {
            val chatId = remoteMessage.data["chatId"] ?: return
            val nombreRemitente = remoteMessage.data["senderName"]
                ?: getString(R.string.chat_notificacion_titulo_defecto)
            val texto = remoteMessage.data["text"] ?: ""
            mostrarNotificacion(this, chatId, nombreRemitente, texto)
        }
    }

    companion object {
        fun mostrarNotificacion(context: Context, chatId: String, titulo: String, texto: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("chatId", chatId)
                putExtra("destino", "chatDetail")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                chatId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificacion = NotificationCompat.Builder(context, TemUESApp.CANAL_MENSAJES_CHAT)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(sonido)
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(chatId.hashCode(), notificacion)
        }
    }
}
