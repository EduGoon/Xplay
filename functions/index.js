"use strict";
/**
 * Import necessary Firebase Admin SDK modules.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK.
admin.initializeApp();

// Import other functions
const authFunctions = require("./auth");
const gameFunctions = require("./game");
const notificationFunctions = require("./notification");

// Export functions
exports.signIn = authFunctions.signIn;
exports.submitMatchResult = gameFunctions.submitMatchResult;
exports.sendNotification = notificationFunctions.sendNotification;
