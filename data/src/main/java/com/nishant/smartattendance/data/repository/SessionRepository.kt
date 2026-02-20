package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.domain.model.AttendanceSession
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class SessionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val sessionsRef = db.collection("attendance_sessions")
    private val attendanceRef = db.collection("attendance")

    // ════════════════════════════════════════
    // ADMIN: Create a session
    // ════════════════════════════════════════

    suspend fun createSession(
        courseId: String,
        section: String,
        semester: Int,
        subject: String
    ): AttendanceSession? {
        return try {
            val code = generateCode()
            val now = System.currentTimeMillis()
            val expiry = now + (15 * 60 * 1000) // 15 minutes
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val sessionId = "${courseId}_${section}_sem${semester}_${subject}_${today}"

            val session = AttendanceSession(
                sessionId = sessionId,
                code = code,
                courseId = courseId,
                section = section,
                semester = semester,
                subject = subject,
                date = today,
                createdAt = now,
                expiresAt = expiry,
                isActive = true
            )

            sessionsRef.document(sessionId).set(
                mapOf(
                    "sessionId" to session.sessionId,
                    "code" to session.code,
                    "courseId" to session.courseId,
                    "section" to session.section,
                    "semester" to session.semester,
                    "subject" to session.subject,
                    "date" to session.date,
                    "createdAt" to session.createdAt,
                    "expiresAt" to session.expiresAt,
                    "isActive" to session.isActive
                )
            ).await()

            session
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deactivateSession(sessionId: String) {
        try {
            sessionsRef.document(sessionId).update("isActive", false).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ════════════════════════════════════════
    // STUDENT: Validate and mark attendance
    // ════════════════════════════════════════

    sealed class MarkResult {
        object Success : MarkResult()
        object InvalidCode : MarkResult()
        object Expired : MarkResult()
        object AlreadyMarked : MarkResult()
        object WrongCourse : MarkResult()
        object SessionInactive : MarkResult()
    }

    suspend fun markAttendanceWithCode(
        enteredCode: String,
        studentSrn: String,
        studentCourseId: String,
        studentSection: String,
        studentSemester: Int,
        studentName: String
    ): MarkResult {
        return try {
            // Find session by code that is active
            val sessionDocs = sessionsRef
                .whereEqualTo("code", enteredCode.trim().uppercase())
                .whereEqualTo("isActive", true)
                .get()
                .await()

            if (sessionDocs.isEmpty) return MarkResult.InvalidCode

            val doc = sessionDocs.documents[0]
            val expiresAt = doc.getLong("expiresAt") ?: 0L
            val isActive = doc.getBoolean("isActive") ?: false
            val courseId = doc.getString("courseId") ?: ""
            val section = doc.getString("section") ?: ""
            val semester = (doc.getLong("semester") ?: 1).toInt()
            val subject = doc.getString("subject") ?: ""
            val date = doc.getString("date") ?: ""
            val sessionId = doc.getString("sessionId") ?: ""

            // Check expiry
            if (System.currentTimeMillis() > expiresAt) {
                // Auto-deactivate expired session
                sessionsRef.document(sessionId).update("isActive", false).await()
                return MarkResult.Expired
            }

            if (!isActive) return MarkResult.SessionInactive

            // Check student belongs to this course/section/semester
            if (courseId != studentCourseId ||
                section != studentSection ||
                semester != studentSemester
            ) return MarkResult.WrongCourse

            // Check already marked for this subject+date
            val alreadyMarked = attendanceRef
                .whereEqualTo("srn", studentSrn)
                .whereEqualTo("subject", subject)
                .whereEqualTo("date", date)
                .get()
                .await()

            if (!alreadyMarked.isEmpty) return MarkResult.AlreadyMarked

            // All checks passed — mark present
            val docId = "${studentSrn}_${courseId}_sem${semester}_${subject}_${date}"
            attendanceRef.document(docId).set(
                mapOf(
                    "srn" to studentSrn,
                    "studentName" to studentName,
                    "courseId" to courseId,
                    "subject" to subject,
                    "section" to section,
                    "semester" to semester,
                    "date" to date,
                    "status" to "present",
                    "markedVia" to "self",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            MarkResult.Success

        } catch (e: Exception) {
            e.printStackTrace()
            MarkResult.InvalidCode
        }
    }

    // ════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I to avoid confusion
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun getRemainingMinutes(expiresAt: Long): Long {
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) remaining / 60000 else 0
    }
}
