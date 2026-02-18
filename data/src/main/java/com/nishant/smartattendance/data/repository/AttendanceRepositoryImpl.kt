package com.nishant.smartattendance.data.repository

import com.nishant.smartattendance.data.remote.FirestoreDataSource
import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.tasks.await

class AttendanceRepositoryImpl(
    private val dataSource: FirestoreDataSource = FirestoreDataSource()
) : AttendanceRepository {

    private val firestore = dataSource.getFirestore()

    override suspend fun markAttendance(record: AttendanceRecord) {

        firestore.collection("attendance")
            .document(record.classId)
            .collection(record.date)
            .document(record.studentId)
            .set(record)
            .await()
    }

    override suspend fun getAttendanceByDate(
        classId: String,
        date: String
    ): List<AttendanceRecord> {

        val snapshot = firestore.collection("attendance")
            .document(classId)
            .collection(date)
            .get()
            .await()

        return snapshot.toObjects(AttendanceRecord::class.java)
    }

    override suspend fun isAttendanceMarked(
        studentId: String,
        date: String
    ): Boolean {

        val snapshot = firestore.collection("attendance")
            .get()
            .await()

        return snapshot.documents.any { it.id == studentId }
    }
}
