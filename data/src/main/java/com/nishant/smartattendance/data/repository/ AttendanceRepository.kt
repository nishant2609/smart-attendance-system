package com.nishant.smartattendance.data.repository

import com.google.firebase.Timestamp
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
                    "${record.srn}_${record.courseId}_${record.date}"
                )
                batch.set(docRef, mapOf(
                    "srn" to record.srn,
                    "studentName" to record.studentName,
                    "courseId" to record.courseId,
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

    suspend fun getTodayAttendanceCount(): Long {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
        return attendanceRef
            .whereEqualTo("date", today)
            .whereEqualTo("status", "present")
            .get()
            .await()
            .size().toLong()
    }

    suspend fun getAttendanceByCourseAndDate(
        courseId: String,
        section: String,
        date: String
    ): List<AttendanceRecord> {
        return attendanceRef
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("section", section)
            .whereEqualTo("date", date)
            .get()
            .await()
            .documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    srn = doc.getString("srn") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    section = doc.getString("section") ?: "",
                    date = doc.getString("date") ?: "",
                    status = doc.getString("status") ?: "absent",
                    markedVia = doc.getString("markedVia") ?: "manual"
                )
            }
    }
}