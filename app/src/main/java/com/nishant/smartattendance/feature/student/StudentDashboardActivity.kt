package com.nishant.smartattendance.feature.student

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nishant.smartattendance.R
import com.nishant.smartattendance.databinding.ActivityStudentDashboardBinding
import com.nishant.smartattendance.feature.notifications.SmartAttendanceMessagingService

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

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // Handle notification tap — navigate to Mark Attendance tab if needed
        handleNotificationIntent(intent)
    }

    // Called when app is already open and a notification is tapped
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra(
            SmartAttendanceMessagingService.EXTRA_NAVIGATE_TO
        ) ?: return

        if (navigateTo == SmartAttendanceMessagingService.NAVIGATE_MARK_ATTENDANCE) {
            // ⚠️ Replace R.id.nav_mark_attendance with the actual ID of your
            // Mark Attendance destination in the student nav graph.
            // Open res/navigation/student_nav_graph.xml and check the fragment id.
            binding.studentBottomNav.selectedItemId = R.id.nav_student_mark
        }
    }
}