package com.nishant.smartattendance.data.repository

import com.nishant.smartattendance.data.remote.FirestoreDataSource
import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.domain.repository.StudentRepository
import kotlinx.coroutines.tasks.await

class StudentRepositoryImpl(
    private val dataSource: FirestoreDataSource = FirestoreDataSource()
) : StudentRepository {

    private val firestore = dataSource.getFirestore()

    override suspend fun addStudent(student: Student) {
        firestore.collection("students")
            .document(student.id)
            .set(student)
            .await()
    }

    override suspend fun getStudentById(id: String): Student? {
        val snapshot = firestore.collection("students")
            .document(id)
            .get()
            .await()

        return snapshot.toObject(Student::class.java)
    }

    override suspend fun getStudentsByClass(classId: String): List<Student> {
        val snapshot = firestore.collection("students")
            .whereEqualTo("classId", classId)
            .get()
            .await()

        return snapshot.toObjects(Student::class.java)
    }

    override suspend fun updateStudent(student: Student) {
        firestore.collection("students")
            .document(student.id)
            .set(student)
            .await()
    }

    override suspend fun deleteStudent(studentId: String) {
        firestore.collection("students")
            .document(studentId)
            .delete()
            .await()
    }
}
