package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.repository.AttendanceRepository

class MarkAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository
) {

    suspend operator fun invoke(record: AttendanceRecord) {

        val alreadyMarked = attendanceRepository.isAttendanceMarked(
            studentId = record.studentId,
            date = record.date
        )

        if (alreadyMarked) {
            throw IllegalStateException("Attendance already marked for today")
        }

        attendanceRepository.markAttendance(record)
    }
}
