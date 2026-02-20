package com.nishant.smartattendance.domain.model

data class AttendanceSession(
    val sessionId: String = "",
    val code: String = "",           // 6-digit code shown to admin
    val courseId: String = "",
    val section: String = "",
    val semester: Int = 1,
    val subject: String = "",
    val date: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,        // createdAt + 15 min
    val isActive: Boolean = true,
    val adminId: String = ""
)
