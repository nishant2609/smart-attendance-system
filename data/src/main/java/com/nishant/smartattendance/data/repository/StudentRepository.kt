package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.domain.model.Student
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class StudentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studentsRef = db.collection("students")

    fun calculateCurrentSemester(joinedAt: Long, totalSemesters: Int): Int {
        if (joinedAt == 0L) return 1
        val now = Calendar.getInstance()
        val joined = Calendar.getInstance().apply { timeInMillis = joinedAt }
        val yearDiff = now.get(Calendar.YEAR) - joined.get(Calendar.YEAR)
        val monthDiff = now.get(Calendar.MONTH) - joined.get(Calendar.MONTH)
        val totalMonths = yearDiff * 12 + monthDiff
        val semester = (totalMonths / 6) + 1
        return semester.coerceIn(1, totalSemesters)
    }

    suspend fun addStudent(student: Student): Boolean {
        return try {
            val joinedAt = System.currentTimeMillis()
            studentsRef.document(student.srn).set(
                mapOf(
                    "srn" to student.srn,
                    "rollNo" to student.rollNo,
                    "name" to student.name,
                    "email" to student.email,
                    "phone" to student.phone,
                    "courseId" to student.courseId,
                    "section" to student.section,
                    "currentSemester" to 1,
                    "joinedAt" to joinedAt,
                    "faceRegistered" to false,
                    "profileComplete" to false,
                    "address" to "",
                    "dateOfBirth" to ""
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateStudentProfile(
        srn: String,
        phone: String,
        address: String,
        dateOfBirth: String
    ): Boolean {
        return try {
            studentsRef.document(srn).update(
                mapOf(
                    "phone" to phone,
                    "address" to address,
                    "dateOfBirth" to dateOfBirth,
                    "profileComplete" to true
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudentByEmail(email: String): Student? {
        val result = studentsRef.whereEqualTo("email", email).get().await()
        if (result.isEmpty) return null
        return mapDocToStudent(result.documents[0])
    }

    suspend fun getStudentBySrn(srn: String): Student? {
        val doc = studentsRef.document(srn).get().await()
        if (!doc.exists()) return null
        return mapDocToStudent(doc)
    }

    suspend fun getStudentsByCourseAndSection(
        courseId: String, section: String
    ): List<Student> {
        return studentsRef
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("section", section)
            .get()
            .await()
            .documents.map { mapDocToStudent(it) }
    }

    suspend fun getTotalStudentsCount(): Long {
        return studentsRef.get().await().size().toLong()
    }

    suspend fun srnExists(srn: String): Boolean {
        return studentsRef.document(srn).get().await().exists()
    }

    private fun mapDocToStudent(doc: com.google.firebase.firestore.DocumentSnapshot): Student {
        return Student(
            uid = doc.id,
            srn = doc.getString("srn") ?: "",
            rollNo = doc.getString("rollNo") ?: "",
            name = doc.getString("name") ?: "",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone") ?: "",
            courseId = doc.getString("courseId") ?: "",
            section = doc.getString("section") ?: "",
            currentSemester = (doc.getLong("currentSemester") ?: 1).toInt(),
            joinedAt = doc.getLong("joinedAt") ?: 0L,
            faceRegistered = doc.getBoolean("faceRegistered") ?: false,
            profileComplete = doc.getBoolean("profileComplete") ?: false,
            address = doc.getString("address") ?: "",
            dateOfBirth = doc.getString("dateOfBirth") ?: ""
        )
    }
}