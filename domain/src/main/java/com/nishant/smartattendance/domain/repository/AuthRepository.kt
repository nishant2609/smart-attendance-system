package com.nishant.smartattendance.domain.repository

interface AuthRepository {

    suspend fun loginWithEmail(email: String, password: String): String

    suspend fun registerWithEmail(email: String, password: String): String

    suspend fun logout()

    fun getCurrentUserId(): String?
}
