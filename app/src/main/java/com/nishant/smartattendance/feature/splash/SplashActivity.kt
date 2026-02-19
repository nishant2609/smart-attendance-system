package com.nishant.smartattendance.feature.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nishant.smartattendance.databinding.ActivitySplashBinding
import com.nishant.smartattendance.feature.admin.AdminDashboardActivity
import com.nishant.smartattendance.feature.auth.LoginActivity
import com.nishant.smartattendance.feature.student.StudentDashboardActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide everything first
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f
        binding.tvCreatedBy.alpha = 0f

        // Delay animation so screen fully loads first
        binding.root.postDelayed({ playAnimation() }, 300)

        lifecycleScope.launch {
            delay(3200)
            navigateNext()
        }
    }

    private fun playAnimation() {
        // Start invisible
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.4f
        binding.ivLogo.scaleY = 0.4f
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = 30f
        binding.tvTagline.alpha = 0f
        binding.tvTagline.translationY = 30f
        binding.tvCreatedBy.alpha = 0f

        // Logo animation
        val logoScaleX = ObjectAnimator.ofFloat(binding.ivLogo, "scaleX", 0.4f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.ivLogo, "scaleY", 0.4f, 1f)
        val logoFade = ObjectAnimator.ofFloat(binding.ivLogo, "alpha", 0f, 1f)
        val logoSet = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoFade)
            duration = 800
            interpolator = OvershootInterpolator(1.2f)
        }

        // App name slide up + fade
        val nameFade = ObjectAnimator.ofFloat(binding.tvAppName, "alpha", 0f, 1f).apply { duration = 600 }
        val nameSlide = ObjectAnimator.ofFloat(binding.tvAppName, "translationY", 30f, 0f).apply { duration = 600 }

        // Tagline slide up + fade
        val taglineFade = ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1f).apply { duration = 600 }
        val taglineSlide = ObjectAnimator.ofFloat(binding.tvTagline, "translationY", 30f, 0f).apply { duration = 600 }

        // Created by fade
        val createdByFade = ObjectAnimator.ofFloat(binding.tvCreatedBy, "alpha", 0f, 1f).apply { duration = 500 }

        AnimatorSet().apply {
            play(logoSet)
            play(nameFade).with(nameSlide).after(500)
            play(taglineFade).with(taglineSlide).after(700)
            play(createdByFade).after(900)
            start()
        }
    }

    private suspend fun navigateNext() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            goTo(LoginActivity::class.java)
        } else {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (doc.getString("role") == "admin") goTo(AdminDashboardActivity::class.java)
                else goTo(StudentDashboardActivity::class.java)
            } catch (e: Exception) {
                goTo(LoginActivity::class.java)
            }
        }
    }

    private fun goTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish()
    }
}