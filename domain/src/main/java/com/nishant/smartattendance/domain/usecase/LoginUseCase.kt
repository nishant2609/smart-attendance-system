package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): String {

        return authRepository.login(email, password)
            ?: throw Exception("Login failed")
    }
}
