package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.domain.model.Course
import kotlinx.coroutines.tasks.await

class CourseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coursesRef = db.collection("courses")

    suspend fun getAllCourses(): List<Course> {
        return coursesRef.get().await().documents.map { doc ->
            Course(
                id = doc.id,
                name = doc.getString("name") ?: "",
                fullName = doc.getString("fullName") ?: "",
                sections = (doc.get("sections") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    suspend fun seedCoursesIfEmpty() {
        val existing = coursesRef.get().await()
        if (!existing.isEmpty) return

        val courses = listOf(
            mapOf(
                "name" to "MCA",
                "fullName" to "Master of Computer Applications",
                "sections" to listOf("A", "B", "C", "D")
            ),
            mapOf(
                "name" to "BCA",
                "fullName" to "Bachelor of Computer Applications",
                "sections" to listOf("A", "B", "C", "D")
            ),
            mapOf(
                "name" to "MBA",
                "fullName" to "Master of Business Administration",
                "sections" to listOf("A", "B", "C", "D")
            ),
            mapOf(
                "name" to "BBA",
                "fullName" to "Bachelor of Business Administration",
                "sections" to listOf("A", "B", "C", "D")
            )
        )

        courses.forEach { coursesRef.add(it).await() }
    }
}