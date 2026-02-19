package com.nishant.smartattendance.feature.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.AttendanceRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentAttendanceBinding
import kotlinx.coroutines.launch

class StudentAttendanceFragment : Fragment() {

    private var _binding: FragmentStudentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSubjectWiseAttendance()
    }

    private fun loadSubjectWiseAttendance() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email) ?: return@launch
                val records = attendanceRepository.getAttendanceBySrn(student.srn)

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