package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(email: String, password: String): String {
        require(email.isNotBlank()) { "Email cannot be empty" }
        require(password.length >= 6) { "Password must be at least 6 characters" }

        return authRepository.registerWithEmail(email, password)
    }
}
