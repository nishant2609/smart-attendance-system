package com.nishant.smartattendance.feature.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nishant.smartattendance.feature.admin.AdminDashboardActivity
import com.nishant.smartattendance.feature.student.StudentDashboardActivity
import com.nishant.smartattendance.di.ServiceLocator
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = ServiceLocator.authRepository.getCurrentUserId()

        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val role = ServiceLocator.userRepository.getUserRole(uid)

            when (role) {
                "admin" -> startActivity(Intent(this@SplashActivity, AdminDashboardActivity::class.java))
                else -> startActivity(Intent(this@SplashActivity, StudentDashboardActivity::class.java))
            }

            finish()
        }
    }
}
