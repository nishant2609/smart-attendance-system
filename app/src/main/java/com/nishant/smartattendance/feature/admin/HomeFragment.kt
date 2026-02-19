package com.nishant.smartattendance.feature.admin

import android.content.Intent
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
import com.nishant.smartattendance.databinding.FragmentHomeBinding
import com.nishant.smartattendance.domain.model.Course
import com.nishant.smartattendance.feature.auth.LoginActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val courseRepository = CourseRepository()
    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                courseRepository.seedCoursesIfEmpty()

                val totalStudents = studentRepository.getTotalStudentsCount()
                val todayPresent = attendanceRepository.getTodayAttendanceCount()
                val courses = courseRepository.getAllCourses()

                binding.tvTotalStudents.text = totalStudents.toString()
                binding.tvTodayPresent.text = todayPresent.toString()

                binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
                binding.rvCourses.adapter = CourseAdapter(courses) { course ->
                    openSubjectManagement(course)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openSubjectManagement(course: Course) {
        val intent = Intent(requireContext(), SubjectManagementActivity::class.java).apply {
            putExtra("COURSE_NAME", course.name)
            putExtra("COURSE_FULL_NAME", course.fullName)
            putStringArrayListExtra("SUBJECTS", ArrayList(course.subjects))
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}