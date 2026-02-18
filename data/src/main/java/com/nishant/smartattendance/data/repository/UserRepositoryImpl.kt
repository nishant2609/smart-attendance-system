package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.core.constants.AdminConfig
import com.nishant.smartattendance.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override suspend fun createUserDocument(
        uid: String,
        email: String
    ) {

        val role = if (AdminConfig.adminEmails.contains(email)) {
            "admin"
        } else {
            "student"
        }

        val userMap = hashMapOf(
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(uid)
            .set(userMap)
            .await()
    }

    override suspend fun getUserRole(uid: String): String? {

        val document = firestore.collection("users")
            .document(uid)
            .get()
            .await()

        return document.getString("role")
    }
}
