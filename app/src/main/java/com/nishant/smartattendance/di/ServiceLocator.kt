package com.nishant.smartattendance.di

import com.nishant.smartattendance.data.repository.AuthRepositoryImpl
import com.nishant.smartattendance.data.repository.UserRepositoryImpl
import com.nishant.smartattendance.domain.repository.AuthRepository
import com.nishant.smartattendance.domain.repository.UserRepository
import com.nishant.smartattendance.domain.usecase.LoginUseCase
import com.nishant.smartattendance.domain.usecase.RegisterUseCase

object ServiceLocator {

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl()
    }

    // UseCases
    val loginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    val registerUseCase by lazy {
        RegisterUseCase(authRepository, userRepository)
    }
}
