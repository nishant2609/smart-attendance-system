package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(email: String, password: String): String {
        require(email.isNotBlank()) { "Email cannot be empty" }
        require(password.isNotBlank()) { "Password cannot be empty" }

        return authRepository.loginWithEmail(email, password)
    }
}
