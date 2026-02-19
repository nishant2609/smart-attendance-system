package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.domain.model.AttendanceRecord
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val attendanceRef = db.collection("attendance")

    suspend fun markAttendance(records: List<AttendanceRecord>): Boolean {
        return try {
            val batch = db.batch()
            records.forEach { record ->
                val docRef = attendanceRef.document(
                    "${record.srn}_${record.courseId}_${record.subject}_${record.date}"
                )
                batch.set(docRef, mapOf(
                    "srn" to record.srn,
                    "studentName" to record.studentName,
                    "courseId" to record.courseId,
                    "subject" to record.subject,
                    "section" to record.section,
                    "date" to record.date,
                    "status" to record.status,
                    "markedVia" to record.markedVia,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAttendanceBySrn(srn: String): List<AttendanceRecord> {
        return attendanceRef
            .whereEqualTo("srn", srn)
            .get()
            .await()
            .documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    srn = doc.getString("srn") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    subject = doc.getString("subject") ?: "",
                    section = doc.getString("section") ?: "",
                    date = doc.getString("date") ?: "",
                    status = doc.getString("status") ?: "absent",
                    markedVia = doc.getString("markedVia") ?: "manual",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }.sortedByDescending { it.date }
    }

    suspend fun getTodayAttendanceCount(): Long {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val records = attendanceRef
            .whereEqualTo("date", today)
            .whereEqualTo("status", "present")
            .get()
            .await()
        // Count unique students, not records
        return records.documents
            .map { it.getString("srn") ?: "" }
            .distinct()
            .size.toLong()
    }
}