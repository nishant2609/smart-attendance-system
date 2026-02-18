package com.nishant.smartattendance.domain.usecase

import com.nishant.smartattendance.domain.repository.AuthRepository
import com.nishant.smartattendance.domain.repository.UserRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ) {

        val uid = authRepository.register(email, password)
            ?: throw Exception("Registration failed")

        userRepository.createUserDocument(uid, email)
    }
}
