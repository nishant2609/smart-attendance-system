package com.nishant.smartattendance.domain.model

data class Student(
    val uid: String = "",
    val srn: String = "",
    val rollNo: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val courseId: String = "",
    val section: String = "",
    val faceRegistered: Boolean = false,
    val address: String = "",
    val profileComplete: Boolean = false
)