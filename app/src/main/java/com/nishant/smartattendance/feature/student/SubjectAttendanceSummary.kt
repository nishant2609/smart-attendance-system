package com.nishant.smartattendance.feature.student

data class SubjectAttendanceSummary(
    val courseName: String,
    val present: Int,
    val total: Int,
    val lastDate: String = ""
) {
    val percentage: Float get() = if (total > 0) (present * 100f / total) else 0f
}
