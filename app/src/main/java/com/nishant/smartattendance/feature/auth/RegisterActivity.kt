package com.nishant.smartattendance.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nishant.smartattendance.MainActivity
import com.nishant.smartattendance.databinding.ActivityRegisterBinding
import com.nishant.smartattendance.di.ServiceLocator
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        lifecycleScope.launch {
            try {
                ServiceLocator.registerUseCase(email, password)
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()
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
