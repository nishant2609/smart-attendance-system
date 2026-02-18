package com.nishant.smartattendance.domain.repository

import com.nishant.smartattendance.domain.model.AttendanceRecord

interface AttendanceRepository {

    suspend fun markAttendance(record: AttendanceRecord)

    suspend fun getAttendanceByDate(classId: String, date: String): List<AttendanceRecord>

    suspend fun isAttendanceMarked(studentId: String, date: String): Boolean
}
