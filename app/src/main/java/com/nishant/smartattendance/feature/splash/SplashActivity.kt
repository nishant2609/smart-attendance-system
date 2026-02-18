package com.nishant.smartattendance.feature.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.feature.admin.AdminDashboardActivity
import com.nishant.smartattendance.feature.auth.LoginActivity
import com.nishant.smartattendance.feature.student.StudentDashboardActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // Not logged in → go to Login
            goTo(LoginActivity::class.java)
        } else {
            // Logged in → check role in Firestore
            lifecycleScope.launch {
                try {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val role = doc.getString("role") ?: "student"

                    if (role == "admin") {
                        goTo(AdminDashboardActivity::class.java)
                    } else {
                        goTo(StudentDashboardActivity::class.java)
                    }
                } catch (e: Exception) {
                    // If Firestore fails, fall back to login
                    goTo(LoginActivity::class.java)
                }
            }
        }
    }

    private fun goTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish()
    }
}