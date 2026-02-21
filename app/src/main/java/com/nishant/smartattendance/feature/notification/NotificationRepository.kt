package com.nishant.smartattendance.feature.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object NotificationRepository {

    private val db = FirebaseFirestore.getInstance()

    // Called on FCM token refresh
    fun saveTokenIfLoggedIn(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("fcm_tokens").document(uid).set(
            mapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    // Call this on login to ensure token is always fresh
    suspend fun refreshAndSaveToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenIfLoggedIn(token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Admin calls this to send a session-start notification to all students
    // in a course/section — done via Firestore trigger or directly via HTTP
    // For now we write a notification_queue doc that a Cloud Function picks up
    suspend fun notifySessionStarted(
        courseId: String,
        section: String,
        semester: Int,
        subject: String,
        code: String,
        expiresAt: Long
    ) {
        try {
            db.collection("notification_queue").add(
                mapOf(
                    "type" to "session_started",
                    "courseId" to courseId,
                    "section" to section,
                    "semester" to semester,
                    "subject" to subject,
                    "code" to code,
                    "expiresAt" to expiresAt,
                    "title" to "📋 Attendance Session Started",
                    "body" to "Your $subject class has started. Enter code $code to mark attendance. Expires in 15 min.",
                    "createdAt" to System.currentTimeMillis(),
                    "processed" to false
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
