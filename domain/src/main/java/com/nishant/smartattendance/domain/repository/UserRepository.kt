package com.nishant.smartattendance.domain.repository

interface UserRepository {

    suspend fun createUserDocument(
        uid: String,
        email: String
    )

    suspend fun getUserRole(uid: String): String?
}
