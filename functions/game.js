
const functions = require("firebase-functions");
const admin = require("firebase-admin");

exports.submitMatchResult = functions.https.onCall(async (data, context) => {
    const { challengeId, result } = data;
    const playerId = context.auth.uid;

    if (!challengeId || !result) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with 'challengeId' and 'result' arguments."
        );
    }

    if (!playerId) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const db = admin.firestore();
    const challengeRef = db.collection("challenges").doc(challengeId);

    try {
        const updatedChallengeData = await db.runTransaction(async (transaction) => {
            const snapshot = await transaction.get(challengeRef);
            if (!snapshot.exists) {
                throw new functions.https.HttpsError("not-found", "Challenge not found!");
            }

            const challenge = snapshot.data();

            if (challenge.status !== "accepted") {
                throw new functions.https.HttpsError("failed-precondition", "This challenge is not active.");
            }

            const playerResultField = playerId === challenge.player1Id ? "player1Result" : "player2Result";
            if (snapshot.get(playerResultField) != null) {
                throw new functions.https.HttpsError("failed-precondition", "You have already submitted your result.");
            }

            transaction.update(challengeRef, { [playerResultField]: result });

            const updatedChallenge = { ...challenge, [playerResultField]: result };

            return updatedChallenge;
        });

        if (updatedChallengeData.player1Result && updatedChallengeData.player2Result) {
            await verifyAndFinalizeMatch(updatedChallengeData);
        }

        return { success: true };

    } catch (error) {
        console.error("Error in submitMatchResult Cloud Function:", error);
        if (error instanceof functions.https.HttpsError) {
            throw error; 
        }
        throw new functions.https.HttpsError("internal", "An unexpected error occurred.");
    }
});

async function verifyAndFinalizeMatch(challenge) {
    const db = admin.firestore();
    const batch = db.batch();

    const p1Result = challenge.player1Result;
    const p2Result = challenge.player2Result;

    if (p1Result === p2Result) {
        batch.update(db.collection("challenges").doc(challenge.challengeId), { status: "disputed" });
        await batch.commit();
        return;
    }

    const winnerId = p1Result === "win" ? challenge.player1Id : challenge.player2Id;
    const loserId = winnerId === challenge.player1Id ? challenge.player2Id : challenge.player1Id;

    const matchRef = db.collection("matches").doc();
    const newMatch = {
        matchid: matchRef.id,
        gameId: challenge.gameId,
        player1Id: challenge.player1Id,
        player2Id: challenge.player2Id,
        winnerId: winnerId
    };
    batch.set(matchRef, newMatch);

    await updatePlayerStatsWithBatch(batch, winnerId, challenge.gameId, 3, 1, 0);
    await updatePlayerStatsWithBatch(batch, loserId, challenge.gameId, -3, 0, 1);

    batch.update(db.collection("challenges").doc(challenge.challengeId), { status: "completed" });

    await batch.commit();
}

async function updatePlayerStatsWithBatch(batch, playerId, gameId, xpChange, winIncrement, lossIncrement) {
    const db = admin.firestore();
    const rankingsQuery = db.collection("rankings").where("playerid", "==", playerId).where("gameid", "==", gameId).limit(1);
    const snapshot = await rankingsQuery.get();

    if (snapshot.empty) {
        const newRankingRef = db.collection("rankings").doc();
        const newRanking = {
            id: newRankingRef.id,
            playerid: playerId,
            gameid: gameId,
            XPpoints: Math.max(0, xpChange),
            wins: winIncrement,
            losses: lossIncrement
        };
        batch.set(newRankingRef, newRanking);
    } else {
        const docRef = snapshot.docs[0].ref;
        const currentData = snapshot.docs[0].data();
        const currentXp = currentData.XPpoints || 0;
        const currentWins = currentData.wins || 0;
        const currentLosses = currentData.losses || 0;

        batch.update(docRef, {
            XPpoints: Math.max(0, currentXp + xpChange),
            wins: currentWins + winIncrement,
            losses: currentLosses + lossIncrement
        });
    }
}
