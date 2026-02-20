package com.nishant.smartattendance.feature.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentAttendanceBinding
import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.model.Course
import com.nishant.smartattendance.domain.model.Student
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val courseRepository = CourseRepository()
    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()

    private var courses = listOf<Course>()
    private var students = listOf<Student>()
    private var selectedCourse: Course? = null
    private var selectedSection: String = "A"
    private var selectedSubject: String = ""
    private var selectedSemester: Int = 1

    // ── Boolean? allows null = "not yet marked" ──
    private val attendanceMap = mutableMapOf<String, Boolean?>()

    // ── Date tracking ──
    private var selectedDate: String = getTodayDate()
    private var isToday: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show today's date automatically
        binding.tvSelectedDate.text = getDisplayDate(selectedDate)

        // Date picker — only allows today or past dates
        binding.btnPickDate.setOnClickListener { showDatePicker() }

        loadCourses()
        binding.btnLoadStudents.setOnClickListener { loadStudentsForAttendance() }
        binding.btnSubmitAttendance.setOnClickListener { submitAttendance() }
    }

    // ════════════════════════════════════════
    // DATE HELPERS
    // ════════════════════════════════════════

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getDisplayDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val display = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            display.format(sdf.parse(dateStr)!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Attendance Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(millis))
            binding.tvSelectedDate.text = getDisplayDate(selectedDate)

            isToday = (selectedDate == getTodayDate())

            // Show read-only warning for past dates
            binding.tvDateNotice.visibility = if (isToday) View.GONE else View.VISIBLE

            // Hide submit for past dates
            binding.btnSubmitAttendance.visibility = View.GONE

            // Clear list until user reloads for new date
            binding.rvAttendance.adapter = null
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    // ════════════════════════════════════════
    // COURSE / SPINNER SETUP
    // ════════════════════════════════════════

    private fun loadCourses() {
        lifecycleScope.launch {
            try {
                courses = courseRepository.getAllCourses()
                val courseNames = courses.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    courseNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCourse.adapter = adapter

                binding.spinnerCourse.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, view: View?, position: Int, id: Long
                        ) {
                            selectedCourse = courses[position]
                            setupSectionSpinner(courses[position].sections)
                            setupSemesterSpinner(courses[position].totalSemesters)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                if (courses.isNotEmpty()) {
                    selectedCourse = courses[0]
                    setupSectionSpinner(courses[0].sections)
                    setupSemesterSpinner(courses[0].totalSemesters)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSectionSpinner(sections: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, sections
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSection.adapter = adapter
        binding.spinnerSection.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) { selectedSection = sections[position] }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupSemesterSpinner(totalSemesters: Int) {
        val semesterList = (1..totalSemesters).map { "Semester $it" }
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, semesterList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSemester.adapter = adapter
        selectedSemester = 1

        binding.spinnerSemester.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedSemester = position + 1
                    loadSubjectsForSemester()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        loadSubjectsForSemester()
    }

    private fun loadSubjectsForSemester() {
        val course = selectedCourse ?: return
        lifecycleScope.launch {
            try {
                val subjects = courseRepository.getSubjectsForSemester(
                    course.name, selectedSemester
                )
                setupSubjectSpinner(subjects)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSubjectSpinner(subjects: List<String>) {
        if (subjects.isEmpty()) {
            Toast.makeText(requireContext(),
                "No subjects for this semester", Toast.LENGTH_SHORT).show()
            return
        }
        selectedSubject = subjects[0]
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, subjects
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubject.adapter = adapter
        binding.spinnerSubject.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) { selectedSubject = subjects[position] }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    // ════════════════════════════════════════
    // LOAD STUDENTS
    // ════════════════════════════════════════

    private fun loadStudentsForAttendance() {
        val course = selectedCourse ?: return
        if (selectedSubject.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a subject", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                students = studentRepository.getStudentsByCourseAndSection(
                    course.name, selectedSection
                ).filter { student ->
                    val currentSem = studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                    currentSem == selectedSemester
                }

                if (students.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "No students found for Semester $selectedSemester",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                attendanceMap.clear()

                // Load existing attendance for selected date (if any)
                val existingRecords = attendanceRepository.getAttendanceForDate(
                    courseId = course.name,
                    section = selectedSection,
                    semester = selectedSemester,
                    subject = selectedSubject,
                    date = selectedDate
                )

                if (existingRecords.isNotEmpty()) {
                    // Pre-fill from saved records
                    existingRecords.forEach { record ->
                        attendanceMap[record.srn] = (record.status == "present")
                    }
                } else {
                    // Fresh session — start with null (nothing selected)
                    students.forEach { attendanceMap[it.srn] = null }
                }

                binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
                binding.rvAttendance.adapter = AttendanceAdapter(
                    students,
                    attendanceMap,
                    isEditable = isToday
                )

                // Only show submit button for today
                binding.btnSubmitAttendance.visibility =
                    if (isToday) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ════════════════════════════════════════
    // SUBMIT
    // ════════════════════════════════════════

    private fun submitAttendance() {
        if (!isToday) {
            Toast.makeText(requireContext(),
                "Cannot edit past attendance", Toast.LENGTH_SHORT).show()
            return
        }

        val course = selectedCourse ?: return

        // Check all students have been marked
        val unmarked = students.count { attendanceMap[it.srn] == null }
        if (unmarked > 0) {
            Toast.makeText(requireContext(),
                "$unmarked student(s) not marked yet. Please mark P or A for all.",
                Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val records = students.map { student ->
                    AttendanceRecord(
                        srn = student.srn,
                        studentName = student.name,
                        courseId = course.name,
                        subject = selectedSubject,
                        section = selectedSection,
                        semester = selectedSemester,
                        date = selectedDate,
                        status = if (attendanceMap[student.srn] == true) "present" else "absent",
                        markedVia = "manual"
                    )
                }

                val success = attendanceRepository.markAttendance(records)
                if (success) {
                    Toast.makeText(requireContext(),
                        "Attendance submitted for $selectedSubject (Sem $selectedSemester)!",
                        Toast.LENGTH_SHORT).show()
                    binding.btnSubmitAttendance.visibility = View.GONE
                    binding.rvAttendance.adapter = null
                } else {
                    Toast.makeText(requireContext(), "Failed to submit", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}