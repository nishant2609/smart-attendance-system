package com.nishant.smartattendance.domain.model

data class AttendanceRecord(
    val id: String = "",
    val srn: String = "",
    val studentName: String = "",
    val courseId: String = "",
    val subject: String = "",
    val section: String = "",
    val semester: Int = 1,
    val date: String = "",
    val status: String = "absent",
    val markedVia: String = "manual",
    val timestamp: Long = 0L
)