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
    private val attendanceMap = mutableMapOf<String, Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCourses()
        binding.btnLoadStudents.setOnClickListener { loadStudentsForAttendance() }
        binding.btnSubmitAttendance.setOnClickListener { submitAttendance() }
    }

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
                    // Only show students in selected semester
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
                students.forEach { attendanceMap[it.srn] = true }
                binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
                binding.rvAttendance.adapter = AttendanceAdapter(students, attendanceMap)
                binding.btnSubmitAttendance.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun submitAttendance() {
        val course = selectedCourse ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                        date = today,
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