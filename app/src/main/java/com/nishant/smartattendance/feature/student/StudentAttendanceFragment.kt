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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudentAttendanceFragment : Fragment() {

    private var _binding: FragmentStudentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()
    private val courseRepository = CourseRepository()

    private var selectedDate: String? = null  // null = all-time summary
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

        // Default hint — no date selected
        binding.tvSelectedDate.text = ""
        binding.tvSelectedDate.hint = "All subjects · All time"

        // Date picker
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        loadSubjectWiseAttendance()
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

            // Reload filtered by date
            loadSubjectWiseAttendance()
        }

        picker.addOnNegativeButtonClickListener {
            // Clear filter — back to all-time
            clearDateFilter()
        }

        picker.show(parentFragmentManager, "STUDENT_DATE_PICKER")
    }

    private fun clearDateFilter() {
        selectedDate = null
        binding.tvSelectedDate.text = ""
        binding.tvSelectedDate.hint = "All subjects · All time"
        loadSubjectWiseAttendance()
    }

    // ════════════════════════════════════════
    // LOAD ATTENDANCE
    // ════════════════════════════════════════

    private fun loadSubjectWiseAttendance() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email) ?: return@launch
                currentSrn = student.srn

                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                // Fetch records — filter by date if one is selected
                val records = attendanceRepository.getAttendanceBySrnAndSemester(
                    student.srn, currentSemester
                ).filter { it.subject.isNotEmpty() }.let { all ->
                    if (selectedDate != null) all.filter { it.date == selectedDate }
                    else all
                }

                if (records.isEmpty() && selectedDate != null) {
                    Toast.makeText(
                        requireContext(),
                        "No attendance records for this date",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Group by subject
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}