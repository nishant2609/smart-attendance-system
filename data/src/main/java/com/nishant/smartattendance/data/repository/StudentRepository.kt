
package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.domain.model.Student
import kotlinx.coroutines.tasks.await

class StudentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studentsRef = db.collection("students")

    suspend fun addStudent(student: Student): Boolean {
        return try {
            studentsRef.document(student.srn).set(
                mapOf(
                    "srn" to student.srn,
                    "rollNo" to student.rollNo,
                    "name" to student.name,
                    "email" to student.email,
                    "phone" to student.phone,
                    "courseId" to student.courseId,
                    "section" to student.section,
                    "faceRegistered" to false,
                    "profileComplete" to false,
                    "address" to ""
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudentsByCourseAndSection(
        courseId: String,
        section: String
    ): List<Student> {
        return studentsRef
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("section", section)
            .get()
            .await()
            .documents.map { doc ->
                Student(
                    uid = doc.id,
                    srn = doc.getString("srn") ?: "",
                    rollNo = doc.getString("rollNo") ?: "",
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    section = doc.getString("section") ?: "",
                    faceRegistered = doc.getBoolean("faceRegistered") ?: false,
                    profileComplete = doc.getBoolean("profileComplete") ?: false
                )
            }
    }

    suspend fun getTotalStudentsCount(): Long {
        return studentsRef.get().await().size().toLong()
    }

    suspend fun srnExists(srn: String): Boolean {
        return studentsRef.document(srn).get().await().exists()
    }
}