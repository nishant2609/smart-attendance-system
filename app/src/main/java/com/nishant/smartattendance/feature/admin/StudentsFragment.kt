package com.nishant.smartattendance.feature.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.nishant.smartattendance.R
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentsBinding
import com.nishant.smartattendance.domain.model.Course
import com.nishant.smartattendance.domain.model.Student
import kotlinx.coroutines.launch

class StudentsFragment : Fragment() {

    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!

    private val courseRepository = CourseRepository()
    private val studentRepository = StudentRepository()

    private var courses = listOf<Course>()
    private var selectedCourse: Course? = null
    private var selectedSection: String = "A"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCourses()

        binding.fabAddStudent.setOnClickListener {
            showAddStudentBottomSheet()
        }
    }

    private fun loadCourses() {
        lifecycleScope.launch {
            try {
                courses = courseRepository.getAllCourses()

                val courseNames = courses.map { it.name }
                val courseAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    courseNames
                )
                courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCourse.adapter = courseAdapter

                binding.spinnerCourse.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, view: View?, position: Int, id: Long
                        ) {
                            selectedCourse = courses[position]
                            setupSectionSpinner(courses[position].sections)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                if (courses.isNotEmpty()) {
                    selectedCourse = courses[0]
                    setupSectionSpinner(courses[0].sections)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSectionSpinner(sections: List<String>) {
        val sectionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sections
        )
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSection.adapter = sectionAdapter

        binding.spinnerSection.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedSection = sections[position]
                    loadStudents()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun loadStudents() {
        val course = selectedCourse ?: return
        lifecycleScope.launch {
            try {
                val students = studentRepository.getStudentsByCourseAndSection(
                    course.name, selectedSection
                )
                binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
                binding.rvStudents.adapter = StudentAdapter(students)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAddStudentBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_student, null)
        dialog.setContentView(sheetView)

        val etSrn = sheetView.findViewById<TextInputEditText>(R.id.etSrn)
        val etRollNo = sheetView.findViewById<TextInputEditText>(R.id.etRollNo)
        val etName = sheetView.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = sheetView.findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = sheetView.findViewById<TextInputEditText>(R.id.etPhone)
        val spinnerCourse = sheetView.findViewById<Spinner>(R.id.spinnerCourse)
        val spinnerSection = sheetView.findViewById<Spinner>(R.id.spinnerSection)
        val btnSave = sheetView.findViewById<Button>(R.id.btnSaveStudent)

        // Setup course spinner in bottom sheet
        val courseNames = courses.map { it.name }
        val courseAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, courseNames
        )
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = courseAdapter

        var sheetSelectedCourse = if (courses.isNotEmpty()) courses[0] else null
        var sheetSelectedSection = "A"

        spinnerCourse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                sheetSelectedCourse = courses[position]
                val sectionAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    courses[position].sections
                )
                sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSection.adapter = sectionAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                sheetSelectedSection = sheetSelectedCourse?.sections?.get(position) ?: "A"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val srn = etSrn.text.toString().trim()
            val rollNo = etRollNo.text.toString().trim()
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (srn.isEmpty() || rollNo.isEmpty() || name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val exists = studentRepository.srnExists(srn)
                    if (exists) {
                        Toast.makeText(requireContext(), "SRN already exists!", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val student = Student(
                        srn = srn,
                        rollNo = rollNo,
                        name = name,
                        email = email,
                        phone = phone,
                        courseId = sheetSelectedCourse?.name ?: "",
                        section = sheetSelectedSection
                    )

                    val success = studentRepository.addStudent(student)
                    if (success) {
                        Toast.makeText(requireContext(), "Student added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadStudents()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add student", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}