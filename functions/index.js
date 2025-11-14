/**
 * Import necessary Firebase Admin SDK modules.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK.
admin.initializeApp();

/**
 * An onCall HTTPS Cloud Function that sends a push notification to a specific user.
 *
 * @param {object} data - The data passed from the client app.
 * @param {string} data.targetUserId - The UID of the user to send the notification to.
 * @param {string} data.title - The title of the notification.
"use strict";
/**
 * Import necessary Firebase Admin SDK modules.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK.
admin.initializeApp();

/**
 * An onCall HTTPS Cloud Function that sends a push notification to a specific user.
 *
 * @param {object} data - The data passed from the client app.
 * @param {string} data.targetUserId - The UID of the user to send the notification to.
 * @param {string} data.title - The title of the notification.
 * @param {string} data.body - The body of the notification.
 * @param {string} [data.requestId] - Optional. The ID for challenge requests.
 * @param {object} context - Metadata about the calling user.
 * @returns {Promise<{success: boolean}>} - A promise that resolves to a success object.
 */
exports.sendNotification = functions.https.onCall(async (data, context) => {
  const { targetUserId, title, body, requestId } = data;

  // 1. Basic validation.
  if (!targetUserId || !title || !body) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The function must be called with 'targetUserId', 'title', and 'body' arguments."
    );
  }

  try {
    // 2. Get the target user's document from the 'players' collection.
    //    IMPORTANT: Assumes your user collection is named 'players'.
    const userDoc = await admin.firestore().collection("players").doc(targetUserId).get();

    if (!userDoc.exists) {
      console.log(`No user document found for userId: ${targetUserId}`);
      return { success: false, error: "User not found." };
    }

    // 3. Get the FCM device token from the user's document.
    //    IMPORTANT: Assumes the field is named 'fcmToken'.
    const fcmToken = userDoc.data().fcmToken;

    if (!fcmToken) {
      console.log(`No fcmToken found for userId: ${targetUserId}`);
      return { success: false, error: "User does not have a device token." };
    }

    // 4. Construct the notification payload.
    //    This is a 'data' message, which ensures onMessageReceived is always called in your app.
    const payload = {
      token: fcmToken,
      data: {
        title: title,
        body: body,
        // Only include requestId if it was provided.
        ...(requestId && { requestId: requestId }),
      },
      // Set Android priority to high to wake the device.
      android: {
        priority: "high",
      },
    };

    // 5. Send the message using the FCM Admin SDK.
    console.log(`Sending notification to token: ${fcmToken}`);
    await admin.messaging().send(payload);

    return { success: true };

  } catch (error) {
    console.error("Error sending notification:", error);
    throw new functions.https.HttpsError(
      "internal",
      "An unexpected error occurred while sending the notification."
    );
  }
});
