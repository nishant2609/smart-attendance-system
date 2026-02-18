package com.nishant.smartattendance.domain.model

data class Student(
    val id: String,
    val name: String,
    val rollNumber: String,
    val classId: String,
    val email: String,
    val faceEmbedding: List<Float>,
    val isClaimed: Boolean
)
