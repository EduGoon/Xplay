const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotification = functions.https.onCall(async (data, context) => {
  const { targetUserId, title, body, requestId } = data;

  // Getting the user's FCM token from Firestore
  const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();
  const fcmToken = userDoc.data().fcmToken;

  if (!fcmToken) {
    throw new functions.https.HttpsError(
      "not-found",
      "FCM token not found for target user."
    );
  }

  const payload = {
    notification: {
      title: title,
      body: body,
    },
    data: {
      requestId: requestId,
      requiresFeedback: "true",
    },
    token: fcmToken,
  };

  try {
    await admin.messaging().send(payload);
    return { success: true };
  } catch (error) {
    console.error("Error sending notification:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Error sending notification."
    );
  }
});
