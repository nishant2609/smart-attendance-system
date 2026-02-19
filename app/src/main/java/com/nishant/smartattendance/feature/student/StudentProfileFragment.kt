package com.nishant.smartattendance.feature.student

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentProfileBinding
import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.feature.auth.LoginActivity
import kotlinx.coroutines.launch
import java.util.Calendar

class StudentProfileFragment : Fragment() {

    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private var currentStudent: Student? = null
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnEditToggle.setOnClickListener {
            enterEditMode()
        }

        binding.btnCancelEdit.setOnClickListener {
            exitEditMode()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Date picker on DOB field click
        binding.etDob.setOnClickListener {
            showDatePicker()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    private fun loadProfile() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email)
                if (student == null) {
                    binding.tvProfileName.text = "Not registered"
                    binding.tvProfileEmail.text = email
                    binding.tvProfileSrn.text = "Contact admin to get registered"
                    return@launch
                }

                currentStudent = student

                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                // Read-only fields
                binding.tvProfileName.text = student.name
                binding.tvProfileSrn.text = "SRN: ${student.srn}"
                binding.tvProfileEmail.text = "Email: ${student.email}"
                binding.tvProfileCourse.text = "Course: ${student.courseId}"
                binding.tvProfileSection.text = "Section: ${student.section} | Roll No: ${student.rollNo}"
                binding.tvProfileSemester.text = "Current Semester: $currentSemester"

                // View mode fields
                binding.tvViewPhone.text = "Phone: ${student.phone.ifEmpty { "Not provided" }}"
                binding.tvViewAddress.text = "Address: ${student.address.ifEmpty { "Not provided" }}"
                binding.tvViewDob.text = "Date of Birth: ${student.dateOfBirth.ifEmpty { "Not provided" }}"

                // Edit mode fields
                binding.etPhone.setText(student.phone)
                binding.etAddress.setText(student.address)
                binding.etDob.setText(student.dateOfBirth)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.cardViewMode.visibility = View.GONE
        binding.cardEditMode.visibility = View.VISIBLE
        binding.btnEditToggle.visibility = View.GONE
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.cardViewMode.visibility = View.VISIBLE
        binding.cardEditMode.visibility = View.GONE
        binding.btnEditToggle.visibility = View.VISIBLE
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Pre-fill if date already set
        val existingDob = binding.etDob.text.toString()
        if (existingDob.isNotEmpty()) {
            try {
                val parts = existingDob.split("/")
                if (parts.size == 3) {
                    calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
            } catch (e: Exception) { }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formatted = "${dayOfMonth.toString().padStart(2, '0')}/${(month + 1).toString().padStart(2, '0')}/$year"
                binding.etDob.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also { picker ->
            // Prevent future dates for DOB
            picker.datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun saveProfile() {
        val student = currentStudent ?: return
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()

        lifecycleScope.launch {
            try {
                val success = studentRepository.updateStudentProfile(
                    srn = student.srn,
                    phone = phone,
                    address = address,
                    dateOfBirth = dob
                )
                if (success) {
                    Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                    exitEditMode()
                    loadProfile()
                } else {
                    Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
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