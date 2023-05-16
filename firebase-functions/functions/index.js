/* eslint-disable linebreak-style */
/* eslint-disable max-len */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/* for notifications: type 0 = reporter | type 1 = cleaner */
/* note : data must only contain strings */

exports.sendNotificationToReporter = functions
    .region("europe-west1")
    .https.onCall(async (data, context) => {
      const reporterId = data.reporterId;
      const cleanerId = data.cleanerId;
      const trashId = data.trashId;
      const description = data.description;
      const imageUrl = data.imageUrl;
      const cleaningRequestId = data.id;

      const fcmToken = await admin
          .firestore()
          .collection("users")
          .doc(reporterId)
          .get()
          .then((doc) => doc.data().fcmToken);

      const message = {
        data: {
          type: "0",
          title: "Demande de vérification de nettoyage",
          cleaningRequestId: cleaningRequestId,
          cleanerId: cleanerId,
          trashId: trashId,
          description: description,
          imageUrl: imageUrl,
        },
        token: fcmToken,
        android: {
          priority: "high",
          ttl: 604800,
        },
      };

      try {
        await admin.messaging().send(message);
        return {success: true};
      } catch (error) {
        console.error("Error sending notification:", error);
        return {success: false};
      }
    });

exports.sendNotificationToCleaner = functions
    .region("europe-west1")
    .https.onCall(async (data, context) => {
      const cleanerId = data.cleanerId;
      const trashId = data.trashId;
      const approved = data.approved;
      const cleaningRequestId = data.id;

      const fcmToken = await admin
          .firestore()
          .collection("users")
          .doc(cleanerId)
          .get()
          .then((doc) => doc.data().fcmToken);

      const notificationTitle = approved ? "Nettoyage approuvé" : "Nettoyage rejeté";
      const notificationBody = approved ? "Votre nettoyage a été approuvé." : "Votre nettoyage a été rejeté.";

      const message = {
        data: {
          type: "1",
          title: notificationTitle,
          body: notificationBody,
        },
        token: fcmToken,
        android: {
          priority: "high",
          ttl: 604800,
        },
      };

      try {
        await admin.messaging().send(message);

        if (approved) {
          await admin
              .firestore()
              .collection("waste")
              .doc(trashId)
              .update({cleaned: true});
        }

        const newStatus = approved ? 1 : 2;
        await admin
            .firestore()
            .collection("cleaningRequests")
            .doc(cleaningRequestId)
            .update({status: newStatus});

        return {success: true};
      } catch (error) {
        console.error("Error sending notification and/or updating trash status:", error);
        return {success: false};
      }
    });
