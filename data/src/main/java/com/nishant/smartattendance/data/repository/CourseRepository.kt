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
                totalSemesters = (doc.getLong("totalSemesters") ?: 4).toInt(),
                sections = (doc.get("sections") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                semesters = parseSemesters(doc.get("semesters"))
            )
        }
    }

    private fun parseSemesters(raw: Any?): Map<String, List<String>> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.entries.associate { (key, value) ->
            key.toString() to ((value as? List<*>)?.filterIsInstance<String>() ?: emptyList())
        }
    }

    suspend fun getSubjectsForSemester(courseId: String, semester: Int): List<String> {
        val doc = coursesRef.whereEqualTo("name", courseId).get().await()
        if (doc.isEmpty) return emptyList()
        val semesters = parseSemesters(doc.documents[0].get("semesters"))
        return semesters[semester.toString()] ?: emptyList()
    }

    suspend fun addSubjectToSemester(
        courseId: String, semester: Int, subject: String
    ): Boolean {
        return try {
            val doc = coursesRef.whereEqualTo("name", courseId).get().await()
            if (doc.isEmpty) return false
            val docRef = doc.documents[0].reference
            val semesters = parseSemesters(
                doc.documents[0].get("semesters")
            ).toMutableMap()
            val subjects = semesters[semester.toString()]?.toMutableList() ?: mutableListOf()
            if (!subjects.contains(subject)) {
                subjects.add(subject)
                semesters[semester.toString()] = subjects
                docRef.update("semesters", semesters).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSubjectFromSemester(
        courseId: String, semester: Int, subject: String
    ): Boolean {
        return try {
            val doc = coursesRef.whereEqualTo("name", courseId).get().await()
            if (doc.isEmpty) return false
            val docRef = doc.documents[0].reference
            val semesters = parseSemesters(
                doc.documents[0].get("semesters")
            ).toMutableMap()
            val subjects = semesters[semester.toString()]?.toMutableList() ?: mutableListOf()
            subjects.remove(subject)
            semesters[semester.toString()] = subjects
            docRef.update("semesters", semesters).await()
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
                "totalSemesters" to 4,
                "sections" to listOf("A", "B", "C", "D"),
                "semesters" to mapOf(
                    "1" to listOf("Mathematics", "Programming in C", "Computer Organization", "Communication Skills", "IT Fundamentals"),
                    "2" to listOf("Data Structures", "Database Management", "Operating Systems", "Python Programming", "Statistics"),
                    "3" to listOf("Computer Networks", "Software Engineering", "Java Programming", "Machine Learning", "Web Technologies"),
                    "4" to listOf("Cloud Computing", "Mobile Development", "Project Work", "Elective", "Research Seminar")
                )
            ),
            mapOf(
                "name" to "BCA",
                "fullName" to "Bachelor of Computer Applications",
                "totalSemesters" to 6,
                "sections" to listOf("A", "B", "C", "D"),
                "semesters" to mapOf(
                    "1" to listOf("Mathematics I", "C Programming", "Computer Fundamentals", "English", "Digital Logic"),
                    "2" to listOf("Mathematics II", "Data Structures", "OOP with C++", "Database Basics", "PC Software"),
                    "3" to listOf("Java Programming", "DBMS", "Operating Systems", "Computer Networks", "Web Design"),
                    "4" to listOf("Python", "Software Engineering", "Linux", "PHP & MySQL", "Computer Graphics"),
                    "5" to listOf("Android Development", "Cloud Computing", "Cyber Security", "AI Basics", "Project I"),
                    "6" to listOf("Machine Learning", "Big Data", "IoT", "Project II", "Elective")
                )
            ),
            mapOf(
                "name" to "MBA",
                "fullName" to "Master of Business Administration",
                "totalSemesters" to 4,
                "sections" to listOf("A", "B", "C", "D"),
                "semesters" to mapOf(
                    "1" to listOf("Management Principles", "Business Economics", "Financial Accounting", "Marketing Management", "Communication"),
                    "2" to listOf("Human Resource Management", "Operations Management", "Business Law", "Research Methods", "Organizational Behavior"),
                    "3" to listOf("Strategic Management", "Financial Management", "Consumer Behavior", "Supply Chain", "Elective I"),
                    "4" to listOf("Entrepreneurship", "Business Ethics", "Project Management", "Elective II", "Dissertation")
                )
            ),
            mapOf(
                "name" to "BBA",
                "fullName" to "Bachelor of Business Administration",
                "totalSemesters" to 6,
                "sections" to listOf("A", "B", "C", "D"),
                "semesters" to mapOf(
                    "1" to listOf("Principles of Management", "Business Economics", "Financial Accounting I", "Business Communication", "IT for Business"),
                    "2" to listOf("Marketing Fundamentals", "Financial Accounting II", "Business Law", "Statistics", "Organizational Behavior"),
                    "3" to listOf("Human Resource Management", "Cost Accounting", "Business Environment", "Consumer Behavior", "Elective I"),
                    "4" to listOf("Financial Management", "Operations Management", "Research Methods", "Entrepreneurship", "Elective II"),
                    "5" to listOf("Strategic Management", "International Business", "Supply Chain", "Project I", "Elective III"),
                    "6" to listOf("Business Ethics", "Corporate Governance", "Project II", "Elective IV", "Internship")
                )
            )
        )

        courses.forEach { coursesRef.add(it).await() }
    }
}