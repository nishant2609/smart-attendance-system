package com.nishant.smartattendance.domain.model

data class Course(
    val id: String = "",
    val name: String = "",
    val fullName: String = "",
    val totalSemesters: Int = 4,
    val sections: List<String> = emptyList(),
    val semesters: Map<String, List<String>> = emptyMap()
)