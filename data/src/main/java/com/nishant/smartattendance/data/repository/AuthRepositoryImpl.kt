package com.nishant.smartattendance.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    override suspend fun loginWithEmail(
        email: String,
        password: String
    ): String {

        val result = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()

        return result.user?.uid
            ?: throw IllegalStateException("Login failed")
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String
    ): String {

        val result = firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .await()

        return result.user?.uid
            ?: throw IllegalStateException("Registration failed")
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
