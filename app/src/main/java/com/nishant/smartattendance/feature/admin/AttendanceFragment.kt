package com.nishant.smartattendance.feature.admin

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.nishant.smartattendance.R
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.SessionRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentAttendanceBinding
import com.nishant.smartattendance.databinding.FragmentSessionCodeDialogBinding
import com.nishant.smartattendance.domain.model.AttendanceRecord
import com.nishant.smartattendance.domain.model.AttendanceSession
import com.nishant.smartattendance.domain.model.Course
import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.feature.notifications.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val courseRepository = CourseRepository()
    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()
    private val sessionRepository = SessionRepository()

    private var courses = listOf<Course>()
    private var students = listOf<Student>()
    private var selectedCourse: Course? = null
    private var selectedSection: String = "A"
    private var selectedSubject: String = ""
    private var selectedSemester: Int = 1

    private val attendanceMap = mutableMapOf<String, Boolean?>()

    private var selectedDate: String = getTodayDate()
    private var isToday: Boolean = true

    private var activeSession: AttendanceSession? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDate.text = getDisplayDate(selectedDate)
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnLoadStudents.setOnClickListener { loadStudentsForAttendance() }
        binding.btnSubmitAttendance.setOnClickListener { submitAttendance() }
        binding.btnStartSession.setOnClickListener { startSession() }
        binding.btnExportAttendance.setOnClickListener {
            findNavController().navigate(R.id.nav_export_attendance)
        }

        loadCourses()
    }

    // ════════════════════════════════════════
    // SESSION MANAGEMENT
    // ════════════════════════════════════════

    private fun startSession() {
        if (selectedSubject.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a subject first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isToday) {
            Toast.makeText(requireContext(), "Sessions can only be started for today", Toast.LENGTH_SHORT).show()
            return
        }
        val course = selectedCourse ?: run {
            Toast.makeText(requireContext(), "Please select a course first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnStartSession.isEnabled = false
                val session = sessionRepository.createSession(
                    courseId = course.name,
                    section = selectedSection,
                    semester = selectedSemester,
                    subject = selectedSubject
                )

                if (session == null) {
                    Toast.makeText(requireContext(), "Failed to create session", Toast.LENGTH_SHORT).show()
                    binding.btnStartSession.isEnabled = true
                    return@launch
                }

                activeSession = session

                // Notify students via FCM queue
                NotificationRepository.notifySessionStarted(
                    courseId = course.name,
                    section = selectedSection,
                    semester = selectedSemester,
                    subject = selectedSubject,
                    code = session.code,
                    expiresAt = session.expiresAt
                )

                showSessionDialog(session)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                binding.btnStartSession.isEnabled = true
            }
        }
    }

    private fun showSessionDialog(session: AttendanceSession) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = FragmentSessionCodeDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)

        dialogBinding.tvSessionCode.text = session.code
        dialogBinding.tvSessionDetails.text =
            "${session.courseId} · ${session.section} · Sem ${session.semester}\n${session.subject}"

        // Live countdown timer
        val remaining = session.expiresAt - System.currentTimeMillis()
        val timer = object : CountDownTimer(remaining.coerceAtLeast(0L), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = millisUntilFinished / 60000
                val secs = (millisUntilFinished % 60000) / 1000
                val color = when {
                    mins < 2 -> 0xFFD32F2F.toInt()  // red — nearly expired
                    mins < 5 -> 0xFFF57C00.toInt()  // orange — running low
                    else     -> 0xFF2E7D32.toInt()  // green — plenty of time
                }
                dialogBinding.tvSessionExpiry.text =
                    "⏱ %02d:%02d remaining".format(mins, secs)
                dialogBinding.tvSessionExpiry.setTextColor(color)
            }
            override fun onFinish() {
                dialogBinding.tvSessionExpiry.text = "⏱ Session expired"
                dialogBinding.tvSessionExpiry.setTextColor(0xFFD32F2F.toInt())
                lifecycleScope.launch {
                    sessionRepository.deactivateSession(session.sessionId)
                    activeSession = null
                    binding.btnStartSession.isEnabled = true
                    dialog.dismiss()
                    Toast.makeText(requireContext(),
                        "Session expired automatically", Toast.LENGTH_SHORT).show()
                    loadStudentsForAttendance()
                }
            }
        }
        timer.start()

        dialogBinding.btnStopSession.setOnClickListener {
            timer.cancel()
            lifecycleScope.launch {
                sessionRepository.deactivateSession(session.sessionId)
                activeSession = null
                binding.btnStartSession.isEnabled = true
                dialog.dismiss()
                Toast.makeText(requireContext(), "Session stopped", Toast.LENGTH_SHORT).show()
                loadStudentsForAttendance()
            }
        }

        dialog.setOnDismissListener {
            timer.cancel()
            binding.btnStartSession.isEnabled = true
        }

        dialog.show()
    }

    // ════════════════════════════════════════
    // DATE HELPERS
    // ════════════════════════════════════════

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun getDisplayDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val display = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            display.format(sdf.parse(dateStr)!!)
        } catch (e: Exception) { dateStr }
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
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
            binding.tvSelectedDate.text = getDisplayDate(selectedDate)
            isToday = (selectedDate == getTodayDate())
            binding.tvDateNotice.visibility = if (isToday) View.GONE else View.VISIBLE
            binding.btnSubmitAttendance.visibility = View.GONE
            binding.btnStartSession.visibility = if (isToday) View.VISIBLE else View.GONE
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
                val adapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_spinner_item, courseNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCourse.adapter = adapter

                binding.spinnerCourse.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupSectionSpinner(sections: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sections)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSection.adapter = adapter
        binding.spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSection = sections[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSemesterSpinner(totalSemesters: Int) {
        val semesterList = (1..totalSemesters).map { "Semester $it" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, semesterList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSemester.adapter = adapter
        selectedSemester = 1

        binding.spinnerSemester.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
                val subjects = courseRepository.getSubjectsForSemester(course.name, selectedSemester)
                setupSubjectSpinner(subjects)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupSubjectSpinner(subjects: List<String>) {
        if (subjects.isEmpty()) return
        selectedSubject = subjects[0]
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subjects)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubject.adapter = adapter
        binding.spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSubject = subjects[position]
            }
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
                students = studentRepository.getStudentsByCourseAndSection(course.name, selectedSection)
                    .filter { student ->
                        val currentSem = studentRepository.calculateCurrentSemester(
                            student.joinedAt, course.totalSemesters)
                        currentSem == selectedSemester
                    }

                if (students.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "No students found for Semester $selectedSemester", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                attendanceMap.clear()
                val existingRecords = attendanceRepository.getAttendanceForDate(
                    courseId = course.name, section = selectedSection,
                    semester = selectedSemester, subject = selectedSubject, date = selectedDate
                )

                if (existingRecords.isNotEmpty()) {
                    existingRecords.forEach { record ->
                        attendanceMap[record.srn] = (record.status == "present")
                    }
                    students.forEach { if (!attendanceMap.containsKey(it.srn)) attendanceMap[it.srn] = null }
                } else {
                    students.forEach { attendanceMap[it.srn] = null }
                }

                binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
                binding.rvAttendance.adapter = AttendanceAdapter(students, attendanceMap, isEditable = isToday)
                binding.btnSubmitAttendance.visibility = if (isToday) View.VISIBLE else View.GONE
                binding.btnStartSession.visibility = if (isToday) View.VISIBLE else View.GONE

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
            Toast.makeText(requireContext(), "Cannot edit past attendance", Toast.LENGTH_SHORT).show()
            return
        }
        val course = selectedCourse ?: return
        val unmarked = students.count { attendanceMap[it.srn] == null }
        if (unmarked > 0) {
            Toast.makeText(requireContext(),
                "$unmarked student(s) not marked yet. Mark P or A for all.",
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
                        "Attendance submitted for $selectedSubject!", Toast.LENGTH_SHORT).show()
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
