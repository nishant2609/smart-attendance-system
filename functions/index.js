/**
 * Smart Attendance — Firebase Cloud Function
 * Triggers when a new doc is written to `notification_queue`.
 * Reads all student FCM tokens for the given course/section/semester,
 * sends FCM push to each, then marks the queue doc as processed.
 *
 * SETUP:
 *   1. cd functions/
 *   2. npm install
 *   3. firebase deploy --only functions
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.sendAttendanceNotification = onDocumentCreated(
  "notification_queue/{docId}",
  async (event) => {
    const db = getFirestore();
    const messaging = getMessaging();

    const snap = event.data;
    if (!snap) return;

    const data = snap.data();

    // Skip if already processed (safety guard against re-triggers)
    if (data.processed === true) return;

    const { courseId, section, semester, subject, code, title, body } = data;

    console.log(`Processing notification for ${courseId} / ${section} / Sem ${semester} / ${subject}`);

    try {
      // ── Step 1: Fetch all student UIDs enrolled in this course/section/semester ──
      // We look up the `users` collection for students matching these criteria.
      // Adjust the field names here to match your Firestore user documents.
      const studentsSnap = await db.collection("users")
        .where("role", "==", "student")
        .where("courseId", "==", courseId)
        .where("section", "==", section)
        .where("semester", "==", semester)
        .get();

      if (studentsSnap.empty) {
        console.log("No students found for this course/section/semester");
        await snap.ref.update({ processed: true, processedAt: Date.now(), sentCount: 0 });
        return;
      }

      const studentUids = studentsSnap.docs.map((doc) => doc.id);
      console.log(`Found ${studentUids.length} students`);

      // ── Step 2: Fetch their FCM tokens ──
      const tokenDocs = await Promise.all(
        studentUids.map((uid) => db.collection("fcm_tokens").doc(uid).get())
      );

      const tokens = tokenDocs
        .filter((doc) => doc.exists && doc.data()?.token)
        .map((doc) => doc.data().token);

      if (tokens.length === 0) {
        console.log("No FCM tokens found for any student");
        await snap.ref.update({ processed: true, processedAt: Date.now(), sentCount: 0 });
        return;
      }

      console.log(`Sending to ${tokens.length} devices`);

      // ── Step 3: Send FCM messages in batches of 500 (FCM limit) ──
      const BATCH_SIZE = 500;
      let successCount = 0;
      let failureCount = 0;

      for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
        const batch = tokens.slice(i, i + BATCH_SIZE);

        const message = {
          tokens: batch,
          notification: {
            title: title || "📋 Attendance Session Started",
            body: body || `Your ${subject} class has started. Enter code ${code} to mark attendance.`,
          },
          data: {
            // Extra data payload — Android app reads this to deep-link to Mark Attendance tab
            type: "session_started",
            courseId: courseId,
            section: section,
            semester: String(semester),
            subject: subject,
            code: code,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "smart_attendance_channel",
              priority: "high",
              defaultSound: true,
              defaultVibrateTimings: true,
              clickAction: "MARK_ATTENDANCE_ACTION",
            },
          },
        };

        const response = await messaging.sendEachForMulticast(message);
        successCount += response.successCount;
        failureCount += response.failureCount;

        // Clean up stale tokens that are no longer valid
        const staleTokenUids = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            const errorCode = resp.error?.code;
            if (
              errorCode === "messaging/invalid-registration-token" ||
              errorCode === "messaging/registration-token-not-registered"
            ) {
              // Find which UID this token belonged to and mark for cleanup
              const tokenDoc = tokenDocs[i + idx];
              if (tokenDoc?.exists) staleTokenUids.push(tokenDoc.id);
            }
          }
        });

        // Delete stale token docs
        if (staleTokenUids.length > 0) {
          const cleanupBatch = db.batch();
          staleTokenUids.forEach((uid) => {
            cleanupBatch.delete(db.collection("fcm_tokens").doc(uid));
          });
          await cleanupBatch.commit();
          console.log(`Cleaned up ${staleTokenUids.length} stale tokens`);
        }
      }

      console.log(`Done. Sent: ${successCount}, Failed: ${failureCount}`);

      // ── Step 4: Mark queue doc as processed ──
      await snap.ref.update({
        processed: true,
        processedAt: Date.now(),
        sentCount: successCount,
        failureCount: failureCount,
      });

    } catch (error) {
      console.error("Error sending notifications:", error);
      // Don't mark as processed so it can be retried if needed
      await snap.ref.update({ error: error.message });
    }
  }
);