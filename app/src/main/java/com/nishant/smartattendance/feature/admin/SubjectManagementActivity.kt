package com.nishant.smartattendance.feature.admin

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.databinding.ActivitySubjectManagementBinding
import kotlinx.coroutines.launch

class SubjectManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectManagementBinding
    private val courseRepository = CourseRepository()

    private lateinit var courseName: String
    private lateinit var courseFullName: String
    private var totalSemesters: Int = 4
    private var selectedSemester: Int = 1
    private lateinit var adapter: SubjectManageAdapter
    private val subjects = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        courseName = intent.getStringExtra("COURSE_NAME") ?: ""
        courseFullName = intent.getStringExtra("COURSE_FULL_NAME") ?: ""
        totalSemesters = intent.getIntExtra("TOTAL_SEMESTERS", 4)

        binding.tvCourseName.text = courseName
        binding.tvCourseFullName.text = courseFullName

        setupSemesterSpinner()

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAddSubject.setOnClickListener { showAddSubjectDialog() }
    }

    private fun setupSemesterSpinner() {
        val semesterList = (1..totalSemesters).map { "Semester $it" }
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, semesterList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSemester.adapter = adapter

        binding.spinnerSemester.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
                ) {
                    selectedSemester = position + 1
                    loadSubjectsForSemester()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        loadSubjectsForSemester()
    }

    private fun loadSubjectsForSemester() {
        lifecycleScope.launch {
            try {
                val loaded = courseRepository.getSubjectsForSemester(
                    courseName, selectedSemester
                )
                subjects.clear()
                subjects.addAll(loaded)

                adapter = SubjectManageAdapter(subjects) { subject, position ->
                    showDeleteConfirmation(subject, position)
                }
                binding.rvSubjects.layoutManager = LinearLayoutManager(this@SubjectManagementActivity)
                binding.rvSubjects.adapter = adapter
                updateSubjectCount()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAddSubjectDialog() {
        val editText = EditText(this).apply {
            hint = "Enter subject name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Subject to $courseName - Semester $selectedSemester")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val newSubject = editText.text.toString().trim()
                if (newSubject.isEmpty()) {
                    Toast.makeText(this, "Subject name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (subjects.contains(newSubject)) {
                    Toast.makeText(this, "Subject already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Add '$newSubject' to $courseName Semester $selectedSemester?")
                    .setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            val success = courseRepository.addSubjectToSemester(
                                courseName, selectedSemester, newSubject
                            )
                            if (success) {
                                adapter.addSubject(newSubject)
                                updateSubjectCount()
                                Toast.makeText(this@SubjectManagementActivity,
                                    "Subject added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SubjectManagementActivity,
                                    "Failed to add", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(subject: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Subject")
            .setMessage("Delete '$subject' from $courseName Semester $selectedSemester?\n\nExisting attendance records will not be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val success = courseRepository.deleteSubjectFromSemester(
                        courseName, selectedSemester, subject
                    )
                    if (success) {
                        adapter.removeSubject(position)
                        updateSubjectCount()
                        Toast.makeText(this@SubjectManagementActivity,
                            "Subject deleted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SubjectManagementActivity,
                            "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSubjectCount() {
        binding.tvSubjectCount.text = "${subjects.size} subjects"
    }
}