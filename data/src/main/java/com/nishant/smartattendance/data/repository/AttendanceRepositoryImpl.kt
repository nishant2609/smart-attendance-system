package com.nishant.smartattendance.data.repository

import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.repository.AttendanceRepository

class AttendanceRepositoryImpl : AttendanceRepository {

    override suspend fun markAttendance(record: AttendanceRecord) {
        // TODO: Firebase logic later
    }

    override suspend fun getAttendanceByDate(
        classId: String,
        date: String
    ): List<AttendanceRecord> {
        return emptyList()
    }

    override suspend fun isAttendanceMarked(
        studentId: String,
        date: String
    ): Boolean {
        return false
    }
}
