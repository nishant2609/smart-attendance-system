package com.nishant.smartattendance.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.databinding.ActivityLoginBinding
import com.nishant.smartattendance.di.ServiceLocator
import com.nishant.smartattendance.feature.admin.AdminDashboardActivity
import com.nishant.smartattendance.feature.student.StudentDashboardActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                ServiceLocator.loginUseCase(email, password)

                // Check role from Firestore
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not found after login")

                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()

                val role = doc.getString("role") ?: "student"

                val destination = if (role == "admin") {
                    AdminDashboardActivity::class.java
                } else {
                    StudentDashboardActivity::class.java
                }

                startActivity(Intent(this@LoginActivity, destination))
                finishAffinity()

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    e.message ?: "Login failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
