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
                    ?.filterIsInstance<String>() ?: emptyList(),
                subjects = (doc.get("subjects") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    suspend fun getSubjectsByCourse(courseId: String): List<String> {
        val doc = coursesRef.whereEqualTo("name", courseId).get().await()
        if (doc.isEmpty) return emptyList()
        return (doc.documents[0].get("subjects") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList()
    }

    suspend fun addSubjectToCourse(courseId: String, subject: String): Boolean {
        return try {
            val doc = coursesRef.whereEqualTo("name", courseId).get().await()
            if (doc.isEmpty) return false
            val docRef = doc.documents[0].reference
            val current = (doc.documents[0].get("subjects") as? List<*>)
                ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            if (!current.contains(subject)) {
                current.add(subject)
                docRef.update("subjects", current).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun seedCoursesIfEmpty() {
        val existing = coursesRef.get().await()
        if (!existing.isEmpty) return

        val courses = listOf(
            mapOf(
                "name" to "MCA",
                "fullName" to "Master of Computer Applications",
                "sections" to listOf("A", "B", "C", "D"),
                "subjects" to listOf(
                    "Database Management Systems",
                    "Operating Systems",
                    "Python Programming",
                    "Computer Networks",
                    "Software Engineering"
                )
            ),
            mapOf(
                "name" to "BCA",
                "fullName" to "Bachelor of Computer Applications",
                "sections" to listOf("A", "B", "C", "D"),
                "subjects" to listOf(
                    "C Programming",
                    "Data Structures",
                    "Web Technologies",
                    "Database Management",
                    "Computer Organization"
                )
            ),
            mapOf(
                "name" to "MBA",
                "fullName" to "Master of Business Administration",
                "sections" to listOf("A", "B", "C", "D"),
                "subjects" to listOf(
                    "Marketing Management",
                    "Financial Accounting",
                    "Human Resource Management",
                    "Business Economics",
                    "Operations Management"
                )
            ),
            mapOf(
                "name" to "BBA",
                "fullName" to "Bachelor of Business Administration",
                "sections" to listOf("A", "B", "C", "D"),
                "subjects" to listOf(
                    "Principles of Management",
                    "Business Communication",
                    "Financial Management",
                    "Marketing Fundamentals",
                    "Business Law"
                )
            )
        )

        courses.forEach { coursesRef.add(it).await() }
    }
}