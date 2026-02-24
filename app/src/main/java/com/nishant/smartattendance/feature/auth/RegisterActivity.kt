package com.nishant.smartattendance.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nishant.smartattendance.core.constants.AdminConfig
import com.nishant.smartattendance.databinding.ActivityRegisterBinding
import com.nishant.smartattendance.di.ServiceLocator
import com.nishant.smartattendance.feature.admin.AdminDashboardActivity
import com.nishant.smartattendance.feature.student.StudentDashboardActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                ServiceLocator.registerUseCase(email, password)

                val destination = if (AdminConfig.adminEmails.contains(email)) {
                    AdminDashboardActivity::class.java
                } else {
                    StudentDashboardActivity::class.java
                }

                startActivity(Intent(this@RegisterActivity, destination))
                finishAffinity() // clears entire back stack so user can't go back to register
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    e.message ?: "Registration failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}