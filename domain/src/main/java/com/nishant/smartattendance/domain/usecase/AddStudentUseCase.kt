package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.domain.repository.StudentRepository

class AddStudentUseCase(
    private val studentRepository: StudentRepository
) {

    suspend operator fun invoke(student: Student) {
        require(student.name.isNotBlank()) { "Student name cannot be empty" }
        require(student.rollNumber.isNotBlank()) { "Roll number cannot be empty" }
        require(student.classId.isNotBlank()) { "Class ID cannot be empty" }

        studentRepository.addStudent(student)
    }
}
