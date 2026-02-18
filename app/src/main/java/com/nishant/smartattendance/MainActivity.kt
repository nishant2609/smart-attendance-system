package com.nishant.smartattendance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nishant.smartattendance.di.ServiceLocator
import com.nishant.smartattendance.domain.model.Student

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example usage (test only)
        val student = Student(
            id = "1",
            name = "Nishant",
            rollNumber = "101",
            classId = "MCA1",
            email = "test@test.com",
            faceEmbedding = emptyList(),
            isClaimed = false
        )

        // Not calling suspend here yet (will fix when we add coroutines)
    }
}
