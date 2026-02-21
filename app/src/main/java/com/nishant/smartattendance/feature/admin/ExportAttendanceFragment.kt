package com.nishant.smartattendance.feature.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.feature.export.ExportRepository
import com.nishant.smartattendance.databinding.FragmentExportAttendanceBinding
import com.nishant.smartattendance.domain.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportAttendanceFragment : Fragment() {

    private var _binding: FragmentExportAttendanceBinding? = null
    private val binding get() = _binding!!

    private val courseRepository = CourseRepository()
    private val attendanceRepository = AttendanceRepository()
    private lateinit var exportRepository: ExportRepository

    private var courses = listOf<Course>()
    private var selectedCourse: Course? = null
    private var selectedSection: String = ""
    private var selectedSemester: Int = 1
    private var selectedSubject: String = ""
    private var fromDate: String = ""
    private var toDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exportRepository = ExportRepository(requireContext())

        binding.btnFromDate.setOnClickListener { pickDate(isFrom = true) }
        binding.btnToDate.setOnClickListener { pickDate(isFrom = false) }
        binding.btnExportPdf.setOnClickListener { exportReport(isPdf = true) }
        binding.btnExportExcel.setOnClickListener { exportReport(isPdf = false) }

        loadCourses()
    }

    // ════════════════════════════════════════
    // DATE PICKERS
    // ════════════════════════════════════════

    private fun pickDate(isFrom: Boolean) {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isFrom) "Select From Date" else "Select To Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val display = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(millis))
            val displayStr = display.format(Date(millis))

            if (isFrom) {
                fromDate = dateStr
                binding.tvFromDate.text = displayStr
            } else {
                toDate = dateStr
                binding.tvToDate.text = displayStr
            }
        }

        picker.show(parentFragmentManager, if (isFrom) "FROM_DATE" else "TO_DATE")
    }

    // ════════════════════════════════════════
    // VALIDATION
    // ════════════════════════════════════════

    private fun validate(): Boolean {
        if (selectedCourse == null) {
            Toast.makeText(requireContext(), "Please select a course", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedSection.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a section", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedSubject.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a subject", Toast.LENGTH_SHORT).show()
            return false
        }
        if (fromDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a from date", Toast.LENGTH_SHORT).show()
            return false
        }
        if (toDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a to date", Toast.LENGTH_SHORT).show()
            return false
        }
        if (fromDate > toDate) {
            Toast.makeText(requireContext(), "From date must be before to date", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // ════════════════════════════════════════
    // EXPORT
    // ════════════════════════════════════════

    private fun exportReport(isPdf: Boolean) {
        if (!validate()) return

        val course = selectedCourse ?: return
        val format = if (isPdf) "PDF" else "Excel"

        binding.layoutProgress.visibility = View.VISIBLE
        binding.tvProgressMessage.text = "Fetching attendance data..."
        binding.btnExportPdf.isEnabled = false
        binding.btnExportExcel.isEnabled = false

        lifecycleScope.launch {
            try {
                // Fetch records from Firestore
                android.util.Log.d("ExportDebug",
                    "Querying: courseId=${course.name}, section=$selectedSection, " +
                            "semester=$selectedSemester, subject=$selectedSubject, " +
                            "from=$fromDate, to=$toDate")

                val records = attendanceRepository.getAttendanceForDateRange(
                    courseId = course.name,
                    section = selectedSection,
                    semester = selectedSemester,
                    subject = selectedSubject,
                    fromDate = fromDate,
                    toDate = toDate
                )

                android.util.Log.d("ExportDebug", "Records found: ${records.size}")

                if (records.isEmpty()) {
                    binding.layoutProgress.visibility = View.GONE
                    resetButtons()
                    Toast.makeText(requireContext(),
                        "No records found.\nCheck Logcat for ExportDebug to verify query values match Firestore.",
                        Toast.LENGTH_LONG).show()
                    return@launch
                }

                binding.tvProgressMessage.text = "Generating $format report..."

                val params = ExportRepository.ExportParams(
                    courseId = course.name,
                    section = selectedSection,
                    semester = selectedSemester,
                    subject = selectedSubject,
                    fromDate = fromDate,
                    toDate = toDate
                )

                // Generate file on IO thread
                val result = withContext(Dispatchers.IO) {
                    if (isPdf) exportRepository.exportToPdf(params, records)
                    else exportRepository.exportToExcel(params, records)
                }

                binding.layoutProgress.visibility = View.GONE
                resetButtons()

                when (result) {
                    is ExportRepository.ExportResult.Success -> {
                        Toast.makeText(requireContext(),
                            "$format report generated!", Toast.LENGTH_SHORT).show()
                        shareFile(result.file, isPdf)
                    }
                    is ExportRepository.ExportResult.Error -> {
                        Toast.makeText(requireContext(),
                            "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                binding.layoutProgress.visibility = View.GONE
                resetButtons()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareFile(file: File, isPdf: Boolean) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val mimeType = if (isPdf) "application/pdf"
            else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - ${selectedSubject}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Attendance Report"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "File saved to Downloads. ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetButtons() {
        binding.btnExportPdf.isEnabled = true
        binding.btnExportExcel.isEnabled = true
    }

    // ════════════════════════════════════════
    // SPINNER SETUP
    // ════════════════════════════════════════

    private fun loadCourses() {
        lifecycleScope.launch {
            try {
                courses = courseRepository.getAllCourses()
                val names = courses.map { it.name }
                val adapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.exportSpinnerCourse.adapter = adapter

                binding.exportSpinnerCourse.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                            selectedCourse = courses[pos]
                            setupSectionSpinner(courses[pos].sections)
                            setupSemesterSpinner(courses[pos].totalSemesters)
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
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, sections)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.exportSpinnerSection.adapter = adapter
        if (sections.isNotEmpty()) selectedSection = sections[0]
        binding.exportSpinnerSection.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    selectedSection = sections[pos]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupSemesterSpinner(totalSemesters: Int) {
        val list = (1..totalSemesters).map { "Semester $it" }
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.exportSpinnerSemester.adapter = adapter
        selectedSemester = 1
        binding.exportSpinnerSemester.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    selectedSemester = pos + 1
                    loadSubjects()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        loadSubjects()
    }

    private fun loadSubjects() {
        val course = selectedCourse ?: return
        lifecycleScope.launch {
            try {
                val subjects = courseRepository.getSubjectsForSemester(course.name, selectedSemester)
                val adapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_spinner_item, subjects)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.exportSpinnerSubject.adapter = adapter
                if (subjects.isNotEmpty()) selectedSubject = subjects[0]
                binding.exportSpinnerSubject.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                            selectedSubject = subjects[pos]
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
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