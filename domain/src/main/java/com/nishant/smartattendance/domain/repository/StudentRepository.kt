package com.nishant.smartattendance.domain.repository

import com.nishant.smartattendance.domain.model.Student

interface StudentRepository {

    suspend fun addStudent(student: Student)

    suspend fun getStudentById(id: String): Student?

    suspend fun getStudentsByClass(classId: String): List<Student>

    suspend fun updateStudent(student: Student)

    suspend fun deleteStudent(studentId: String)
}
