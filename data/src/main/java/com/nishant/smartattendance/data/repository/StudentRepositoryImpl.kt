package com.nishant.smartattendance.data.repository

import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.domain.repository.StudentRepository

class StudentRepositoryImpl : StudentRepository {

    override suspend fun addStudent(student: Student) {
        // TODO: Implement Firebase logic later
    }

    override suspend fun getStudentById(id: String): Student? {
        return null
    }

    override suspend fun getStudentsByClass(classId: String): List<Student> {
        return emptyList()
    }

    override suspend fun updateStudent(student: Student) {
        // TODO
    }

    override suspend fun deleteStudent(studentId: String) {
        // TODO
    }
}
