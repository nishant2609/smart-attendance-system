package com.nishant.smartattendance.feature.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.ActivityStudentDetailBinding
import kotlinx.coroutines.launch

class StudentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDetailBinding
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val srn = intent.getStringExtra("SRN") ?: return
        binding.btnBack.setOnClickListener { finish() }
        loadStudent(srn)
    }

    private fun loadStudent(srn: String) {
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentBySrn(srn) ?: return@launch
                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                // Academic info
                binding.tvName.text = student.name
                binding.tvSrn.text = "SRN: ${student.srn}"
                binding.tvRollNo.text = "Roll No: ${student.rollNo}"
                binding.tvEmail.text = "Email: ${student.email}"
                binding.tvCourse.text = "Course: ${student.courseId}"
                binding.tvSection.text = "Section: ${student.section}"
                binding.tvSemester.text = "Current Semester: $currentSemester"

                // Contact info
                binding.tvPhone.text = "Phone: ${student.phone.ifEmpty { "Not provided" }}"
                binding.tvAddress.text = "Address: ${student.address.ifEmpty { "Not provided" }}"
                binding.tvDob.text = "Date of Birth: ${student.dateOfBirth.ifEmpty { "Not provided" }}"

                // Status
                if (student.faceRegistered) {
                    binding.tvFaceStatus.text = "✓ Face Registered"
                    binding.tvFaceStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
                    binding.tvFaceStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    binding.tvFaceStatus.text = "✗ No Face"
                    binding.tvFaceStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
                    binding.tvFaceStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }

                if (student.profileComplete) {
                    binding.tvStudentProfileStatus.text = "✓ Profile Complete"
                    binding.tvStudentProfileStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
                    binding.tvStudentProfileStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    binding.tvStudentProfileStatus.text = "✗ Profile Incomplete"
                    binding.tvStudentProfileStatus.setBackgroundColor(getColor(android.R.color.holo_orange_light))
                    binding.tvStudentProfileStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}