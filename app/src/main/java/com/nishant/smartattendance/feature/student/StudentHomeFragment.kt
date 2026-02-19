package com.nishant.smartattendance.feature.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentHomeBinding
import com.nishant.smartattendance.domain.model.Student
import kotlinx.coroutines.launch

class StudentHomeFragment : Fragment() {

    private var _binding: FragmentStudentHomeBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()
    private val courseRepository = CourseRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStudentData()
    }

    private fun loadStudentData() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email)
                if (student == null) {
                    binding.tvWelcome.text = "Welcome 👋"
                    binding.tvCourseInfo.text = "Profile not set up yet. Contact admin."
                    return@launch
                }

                // Get course to calculate current semester
                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                displayStudentInfo(student, currentSemester)
                loadAttendanceStats(student, currentSemester)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayStudentInfo(student: Student, currentSemester: Int) {
        binding.tvWelcome.text = "Welcome, ${student.name} 👋"
        binding.tvCourseInfo.text = "${student.courseId} | Sem $currentSemester | Section ${student.section} | Roll: ${student.rollNo}"
    }

    private suspend fun loadAttendanceStats(student: Student, currentSemester: Int) {
        val records = attendanceRepository.getAttendanceBySrnAndSemester(
            student.srn, currentSemester
        ).filter { it.subject.isNotEmpty() }

        val totalClasses = records.size
        val presentCount = records.count { it.status == "present" }
        val percentage = if (totalClasses == 0) 0 else (presentCount * 100) / totalClasses

        binding.tvAttendancePercent.text = "$percentage%"
        binding.tvClassesAttended.text = presentCount.toString()

        binding.rvRecentAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentAttendance.adapter = AttendanceRecordAdapter(records.take(5))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}