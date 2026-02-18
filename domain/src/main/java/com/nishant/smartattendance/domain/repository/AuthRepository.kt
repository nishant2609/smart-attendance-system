package com.nishant.smartattendance.domain.repository

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): String?

    suspend fun register(
        email: String,
        password: String
    ): String?

    fun getCurrentUserId(): String?

    fun logout()
}
