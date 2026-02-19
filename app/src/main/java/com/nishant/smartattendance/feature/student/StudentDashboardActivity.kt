package com.nishant.smartattendance.feature.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nishant.smartattendance.R
import com.nishant.smartattendance.databinding.ActivityStudentDashboardBinding

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.studentNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.studentBottomNav.setupWithNavController(navController)
    }
}
