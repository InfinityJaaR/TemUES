const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

exports.enviarNotificacionChat = functions.firestore
  .document("users/{userId}/notifications/{notifId}")
  .onCreate(async (snap, context) => {
    const userId = context.params.userId;
    const datos = snap.data();

    if (!datos || datos.type !== "chat_message") return null;

    const senderId = datos.senderId || "";
    if (senderId === userId) return snap.ref.delete();

    const userSnap = await admin.firestore()
      .collection("users")
      .doc(userId)
      .get();

    const fcmToken = userSnap.data()?.fcmToken;
    if (!fcmToken) return snap.ref.delete();

    const mensaje = {
      token: fcmToken,
      data: {
        type: "chat_message",
        chatId: datos.chatId || "",
        senderName: datos.senderName || "",
        text: datos.text || "",
        senderId: senderId,
      },
      android: {
        priority: "high",
      },
    };

    try {
      await admin.messaging().send(mensaje);
    } catch (e) {
      console.error("Error enviando FCM:", e);
    }

    return snap.ref.delete();
  });
