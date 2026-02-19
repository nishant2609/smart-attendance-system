package com.nishant.smartattendance.feature.admin

import android.os.Bundle
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
    private lateinit var adapter: SubjectManageAdapter
    private val subjects = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        courseName = intent.getStringExtra("COURSE_NAME") ?: ""
        courseFullName = intent.getStringExtra("COURSE_FULL_NAME") ?: ""
        val initialSubjects = intent.getStringArrayListExtra("SUBJECTS") ?: arrayListOf()

        subjects.addAll(initialSubjects)

        binding.tvCourseName.text = courseName
        binding.tvCourseFullName.text = courseFullName
        updateSubjectCount()

        adapter = SubjectManageAdapter(subjects,
            onDeleteClick = { subject, position ->
                showDeleteConfirmation(subject, position)
            }
        )
        binding.rvSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvSubjects.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        binding.fabAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }
    }

    private fun showAddSubjectDialog() {
        val editText = EditText(this).apply {
            hint = "Enter subject name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Subject to $courseName")
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
                // Confirm before adding
                AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Add '$newSubject' to $courseName?")
                    .setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            val success = courseRepository.addSubjectToCourse(courseName, newSubject)
                            if (success) {
                                adapter.addSubject(newSubject)
                                updateSubjectCount()
                                Toast.makeText(this@SubjectManagementActivity,
                                    "Subject added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SubjectManagementActivity,
                                    "Failed to add subject", Toast.LENGTH_SHORT).show()
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
            .setMessage("Are you sure you want to delete '$subject' from $courseName?\n\nThis will not delete existing attendance records.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val success = courseRepository.deleteSubjectFromCourse(courseName, subject)
                    if (success) {
                        adapter.removeSubject(position)
                        updateSubjectCount()
                        Toast.makeText(this@SubjectManagementActivity,
                            "Subject deleted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SubjectManagementActivity,
                            "Failed to delete subject", Toast.LENGTH_SHORT).show()
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