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
import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.model.Student
import com.facebook.shimmer.ShimmerFrameLayout
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

        showShimmer()
        loadStudentData()
    }

    // ════════════════════════════════════════
    // SHIMMER
    // ════════════════════════════════════════

    private fun showShimmer() {
        val shimmer = binding.shimmerStudentHome.root as ShimmerFrameLayout
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        binding.layoutContent.visibility = View.GONE
    }

    private fun hideShimmer() {
        val shimmer = binding.shimmerStudentHome.root as ShimmerFrameLayout
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
    }

    // ════════════════════════════════════════
    // LOAD DATA
    // ════════════════════════════════════════

    private fun loadStudentData() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email)
                if (student == null) {
                    hideShimmer()
                    binding.tvWelcome.text = "Welcome 👋"
                    binding.tvCourseInfo.text = "Profile not set up yet. Contact admin."
                    return@launch
                }

                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                val records = attendanceRepository.getAttendanceBySrnAndSemester(
                    student.srn, currentSemester
                ).filter { it.subject.isNotEmpty() }

                hideShimmer()

                displayStudentInfo(student, currentSemester)
                loadAttendanceStats(records)
                checkAttendanceWarning(records)

            } catch (e: Exception) {
                e.printStackTrace()
                hideShimmer()
            }
        }
    }

    private fun displayStudentInfo(student: Student, currentSemester: Int) {
        binding.tvWelcome.text = "Welcome, ${student.name} 👋"
        binding.tvCourseInfo.text =
            "${student.courseId} | Sem $currentSemester | Section ${student.section} | Roll: ${student.rollNo}"
    }

    // ════════════════════════════════════════
    // ATTENDANCE STATS + EMPTY STATE
    // ════════════════════════════════════════

    private fun loadAttendanceStats(records: List<AttendanceRecord>) {
        val totalClasses = records.size
        val presentCount = records.count { it.status == "present" }
        val percentage = if (totalClasses == 0) 0 else (presentCount * 100) / totalClasses

        binding.tvAttendancePercent.text = "$percentage%"
        binding.tvClassesAttended.text = presentCount.toString()

        val recent = records.take(5)
        if (recent.isEmpty()) {
            binding.rvRecentAttendance.visibility = View.GONE
            binding.emptyRecent.root.visibility = View.VISIBLE
        } else {
            binding.rvRecentAttendance.visibility = View.VISIBLE
            binding.emptyRecent.root.visibility = View.GONE
            binding.rvRecentAttendance.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentAttendance.adapter = AttendanceRecordAdapter(recent)
        }
    }

    // ════════════════════════════════════════
    // ATTENDANCE WARNING (< 75% in any subject)
    // ════════════════════════════════════════

    private fun checkAttendanceWarning(records: List<AttendanceRecord>) {
        if (records.isEmpty()) {
            binding.cardWarning.visibility = View.GONE
            return
        }

        // Group by subject and check each one
        val subjectMap = records.groupBy { it.subject }
        val lowSubjects = subjectMap.filter { (_, subjectRecords) ->
            val present = subjectRecords.count { it.status == "present" }
            val total = subjectRecords.size
            if (total == 0) false
            else (present * 100) / total < 75
        }.keys.toList()

        if (lowSubjects.isEmpty()) {
            binding.cardWarning.visibility = View.GONE
        } else {
            binding.cardWarning.visibility = View.VISIBLE
            val subjectText = if (lowSubjects.size == 1) {
                "${lowSubjects[0]} is below 75%"
            } else {
                "${lowSubjects.size} subjects below 75%: ${lowSubjects.joinToString(", ")}"
            }
            binding.tvWarningMessage.text = subjectText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}