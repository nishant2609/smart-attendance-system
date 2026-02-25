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

    // ════════════════════════════════════════
    // LOAD DATA
    // ════════════════════════════════════════

    private fun loadData() {
        lifecycleScope.launch {
            try {
                courseRepository.seedCoursesIfEmpty()

                val totalStudents = studentRepository.getTotalStudentsCount()
                val todayPresent = attendanceRepository.getTodayAttendanceCount()
                val courses = courseRepository.getAllCourses()

                binding.tvTotalStudents.text = totalStudents.toString()
                binding.tvTodayPresent.text = todayPresent.toString()

                // Low attendance alerts — students below 75% in any subject
                loadLowAttendanceAlerts()

                if (courses.isEmpty()) {
                    binding.rvCourses.visibility = View.GONE
                } else {
                    binding.rvCourses.visibility = View.VISIBLE
                    binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvCourses.adapter = CourseAdapter(courses) { course ->
                        openSubjectManagement(course)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ════════════════════════════════════════
    // LOW ATTENDANCE ALERTS
    // ════════════════════════════════════════

    private fun loadLowAttendanceAlerts() {
        lifecycleScope.launch {
            try {
                val allStudents = studentRepository.getAllStudents()
                val alerts = mutableListOf<LowAttendanceAlert>()

                for (student in allStudents) {
                    val records = attendanceRepository.getAttendanceBySrnAndSemester(
                        student.srn, student.currentSemester
                    ).filter { it.subject.isNotEmpty() }

                    val grouped = records.groupBy { it.subject }
                    for ((subject, subjectRecords) in grouped) {
                        val total = subjectRecords.size
                        if (total < 3) continue  // skip subjects with too few classes
                        val present = subjectRecords.count { it.status == "present" }
                        val pct = present * 100f / total
                        if (pct < 75f) {
                            alerts.add(LowAttendanceAlert(
                                studentName = student.name,
                                srn = student.srn,
                                subject = subject,
                                percentage = pct
                            ))
                        }
                    }
                }

                if (alerts.isEmpty()) {
                    binding.cardLowAttendance.visibility = View.GONE
                } else {
                    val sorted = alerts.sortedBy { it.percentage }
                    val displayList = sorted.take(10)
                    binding.cardLowAttendance.visibility = View.VISIBLE
                    binding.tvLowAttendanceCount.text = "${alerts.size} alert(s)"
                    binding.rvLowAttendance.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvLowAttendance.adapter = LowAttendanceAdapter(displayList)
                }
            } catch (e: Exception) {
                binding.cardLowAttendance.visibility = View.GONE
            }
        }
    }

    // ════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════

    private fun openSubjectManagement(course: Course) {
        val intent = Intent(requireContext(), SubjectManagementActivity::class.java).apply {
            putExtra("COURSE_NAME", course.name)
            putExtra("COURSE_FULL_NAME", course.fullName)
            putExtra("TOTAL_SEMESTERS", course.totalSemesters)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}