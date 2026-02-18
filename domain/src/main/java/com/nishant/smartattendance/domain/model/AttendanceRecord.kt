package com.nishant.smartattendance.domain.model

data class AttendanceRecord(
    val id: String,
    val studentId: String,
    val classId: String,
    val date: String,
    val isPresent: Boolean
)
