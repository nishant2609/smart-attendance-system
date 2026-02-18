package com.nishant.smartattendance.di

import com.nishant.smartattendance.data.repository.AttendanceRepositoryImpl
import com.nishant.smartattendance.data.repository.StudentRepositoryImpl
import com.nishant.smartattendance.domain.repository.AttendanceRepository
import com.nishant.smartattendance.domain.repository.StudentRepository
import com.nishant.smartattendance.domain.usecase.AddStudentUseCase
import com.nishant.smartattendance.domain.usecase.MarkAttendanceUseCase
import com.nishant.smartattendance.data.repository.AuthRepositoryImpl
import com.nishant.smartattendance.domain.repository.AuthRepository
import com.nishant.smartattendance.domain.usecase.LoginUseCase
import com.nishant.smartattendance.domain.usecase.RegisterUseCase


object ServiceLocator {

    // Repositories
    val studentRepository: StudentRepository by lazy {
        StudentRepositoryImpl()
    }

    val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl()
    }

    // UseCases
    val addStudentUseCase: AddStudentUseCase by lazy {
        AddStudentUseCase(studentRepository)
    }

    val markAttendanceUseCase: MarkAttendanceUseCase by lazy {
        MarkAttendanceUseCase(attendanceRepository)
    }

    // Auth Repository
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    // Auth UseCases
    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    val registerUseCase: RegisterUseCase by lazy {
        RegisterUseCase(authRepository)
    }


}
