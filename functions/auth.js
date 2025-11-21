
const functions = require("firebase-functions");
const admin = require("firebase-admin");

exports.signIn = functions.https.onCall(async (data, context) => {
    const idToken = data.idToken;
    if (!idToken) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called with an idToken."
        );
    }

    try {
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        const uid = decodedToken.uid;
        const userRecord = await admin.auth().getUser(uid);
        const user = userRecord.toJSON();

        const userDocRef = admin.firestore().collection("players").doc(uid);
        const userDoc = await userDocRef.get();

        if (!userDoc.exists) {
            console.log("New user detected, creating profile...");
            const newPlayer = {
                uid: uid,
                name: user.displayName,
                email: user.email,
                profilePictureUrl: user.photoUrl,
                isFirstTime: true
            };
            await userDocRef.set(newPlayer);
            console.log(`New user profile created in Firestore: ${uid}`);
            return newPlayer;
        } else {
            console.log("Returning user detected, fetching profile...");
            return userDoc.data();
        }
    } catch (error) {
        console.error("Error in signIn Cloud Function:", error);
        throw new functions.https.HttpsError(
            "internal",
            "An unexpected error occurred during sign-in."
        );
    }
});
