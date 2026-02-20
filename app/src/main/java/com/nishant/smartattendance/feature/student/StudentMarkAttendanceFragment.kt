package com.nishant.smartattendance.feature.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.SessionRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.databinding.FragmentMarkAttendanceBinding
import kotlinx.coroutines.launch

class StudentMarkAttendanceFragment : Fragment() {

    private var _binding: FragmentMarkAttendanceBinding? = null
    private val binding get() = _binding!!

    private val sessionRepository = SessionRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardStatus.visibility = View.GONE

        binding.btnMarkAttendance.setOnClickListener {
            val code = binding.etClassCode.text?.toString()?.trim()?.uppercase() ?: ""
            if (code.length < 6) {
                showStatus(
                    isSuccess = false,
                    icon = "⚠️",
                    title = "Invalid Code",
                    message = "Please enter the full 6-character class code"
                )
                return@setOnClickListener
            }
            markAttendance(code)
        }
    }

    private fun markAttendance(code: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etClassCode.windowToken, 0)

        binding.btnMarkAttendance.isEnabled = false
        binding.btnMarkAttendance.alpha = 0.6f

        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email)
                if (student == null) {
                    showStatus(
                        isSuccess = false,
                        icon = "❌",
                        title = "Profile Not Found",
                        message = "Your student profile is not set up. Contact admin."
                    )
                    resetButton()
                    return@launch
                }

                // Get current semester
                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(
                        student.joinedAt, course.totalSemesters
                    )
                } else student.currentSemester

                val result = sessionRepository.markAttendanceWithCode(
                    enteredCode = code,
                    studentSrn = student.srn,
                    studentCourseId = student.courseId,
                    studentSection = student.section,
                    studentSemester = currentSemester,
                    studentName = student.name
                )

                when (result) {
                    is SessionRepository.MarkResult.Success -> {
                        showStatus(
                            isSuccess = true,
                            icon = "✅",
                            title = "Attendance Marked!",
                            message = "Your attendance has been recorded successfully"
                        )
                        binding.etClassCode.text?.clear()
                    }
                    is SessionRepository.MarkResult.InvalidCode -> {
                        showStatus(
                            isSuccess = false,
                            icon = "❌",
                            title = "Invalid Code",
                            message = "No active session found for this code. Check the code and try again."
                        )
                    }
                    is SessionRepository.MarkResult.Expired -> {
                        showStatus(
                            isSuccess = false,
                            icon = "⏰",
                            title = "Code Expired",
                            message = "This session code has expired. Ask your teacher for a new one."
                        )
                    }
                    is SessionRepository.MarkResult.AlreadyMarked -> {
                        showStatus(
                            isSuccess = false,
                            icon = "ℹ️",
                            title = "Already Marked",
                            message = "Your attendance is already recorded for this class today."
                        )
                    }
                    is SessionRepository.MarkResult.WrongCourse -> {
                        showStatus(
                            isSuccess = false,
                            icon = "🚫",
                            title = "Wrong Class",
                            message = "This session is for a different course, section, or semester."
                        )
                    }
                    is SessionRepository.MarkResult.SessionInactive -> {
                        showStatus(
                            isSuccess = false,
                            icon = "🔒",
                            title = "Session Closed",
                            message = "This session has been closed by the teacher."
                        )
                    }
                }

            } catch (e: Exception) {
                showStatus(
                    isSuccess = false,
                    icon = "❌",
                    title = "Error",
                    message = e.message ?: "Something went wrong. Please try again."
                )
            }

            resetButton()
        }
    }

    private fun showStatus(isSuccess: Boolean, icon: String, title: String, message: String) {
        binding.cardStatus.visibility = View.VISIBLE
        binding.tvStatusIcon.text = icon
        binding.tvStatusTitle.text = title
        binding.tvStatusTitle.setTextColor(
            if (isSuccess) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt()
        )
        binding.tvStatusMessage.text = message

        // Card accent color
        binding.cardStatus.setCardBackgroundColor(
            if (isSuccess) 0xFFF1F8E9.toInt() else 0xFFFFF3F3.toInt()
        )
    }

    private fun resetButton() {
        binding.btnMarkAttendance.isEnabled = true
        binding.btnMarkAttendance.alpha = 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
