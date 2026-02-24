package com.nishant.smartattendance.feature.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentAttendanceBinding
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudentAttendanceFragment : Fragment() {

    private var _binding: FragmentStudentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()
    private val courseRepository = CourseRepository()

    private var selectedDate: String? = null
    private var currentSrn: String = ""
    private var currentSemester: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDate.text = ""
        binding.tvSelectedDate.hint = "All subjects · All time"

        binding.btnPickDate.setOnClickListener { showDatePicker() }

        showShimmer()
        loadSubjectWiseAttendance()
    }

    // ════════════════════════════════════════
    // SHIMMER
    // ════════════════════════════════════════

    private fun showShimmer() {
        val shimmer = binding.shimmerSubjectAttendance.root as ShimmerFrameLayout
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        binding.rvSubjectAttendance.visibility = View.GONE
        binding.emptySubjects.root.visibility = View.GONE
    }

    private fun hideShimmer() {
        val shimmer = binding.shimmerSubjectAttendance.root as ShimmerFrameLayout
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
    }

    // ════════════════════════════════════════
    // DATE PICKER
    // ════════════════════════════════════════

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Filter by Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = sdf.format(Date(millis))
            val display = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            binding.tvSelectedDate.text = display.format(Date(millis))
            binding.tvSelectedDate.hint = ""
            showShimmer()
            loadSubjectWiseAttendance()
        }

        picker.addOnNegativeButtonClickListener {
            clearDateFilter()
        }

        picker.show(parentFragmentManager, "STUDENT_DATE_PICKER")
    }

    private fun clearDateFilter() {
        selectedDate = null
        binding.tvSelectedDate.text = ""
        binding.tvSelectedDate.hint = "All subjects · All time"
        showShimmer()
        loadSubjectWiseAttendance()
    }

    // ════════════════════════════════════════
    // LOAD ATTENDANCE
    // ════════════════════════════════════════

    private fun loadSubjectWiseAttendance() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email) ?: run {
                    hideShimmer()
                    return@launch
                }
                currentSrn = student.srn

                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                val records = attendanceRepository.getAttendanceBySrnAndSemester(
                    student.srn, currentSemester
                ).filter { it.subject.isNotEmpty() }.let { all ->
                    if (selectedDate != null) all.filter { it.date == selectedDate } else all
                }

                hideShimmer()

                if (records.isEmpty()) {
                    binding.rvSubjectAttendance.visibility = View.GONE
                    binding.emptySubjects.root.visibility = View.VISIBLE
                    if (selectedDate != null) {
                        Toast.makeText(requireContext(),
                            "No records for this date", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                binding.emptySubjects.root.visibility = View.GONE
                binding.rvSubjectAttendance.visibility = View.VISIBLE

                val grouped = records.groupBy { it.subject }
                val summaries = grouped.map { (subject, subjectRecords) ->
                    SubjectAttendanceSummary(
                        courseName = subject,
                        present = subjectRecords.count { it.status == "present" },
                        total = subjectRecords.size
                    )
                }.sortedBy { it.courseName }

                binding.rvSubjectAttendance.layoutManager = LinearLayoutManager(requireContext())
                binding.rvSubjectAttendance.adapter = SubjectAttendanceAdapter(summaries)

            } catch (e: Exception) {
                e.printStackTrace()
                hideShimmer()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}