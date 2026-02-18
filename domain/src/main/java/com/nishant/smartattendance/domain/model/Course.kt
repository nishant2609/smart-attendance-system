package com.nishant.smartattendance.domain.model

data class Course(
    val id: String = "",
    val name: String = "",
    val fullName: String = "",
    val sections: List<String> = emptyList()
)