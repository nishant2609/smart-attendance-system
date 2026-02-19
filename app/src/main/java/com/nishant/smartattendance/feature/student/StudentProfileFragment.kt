package com.nishant.smartattendance.feature.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentProfileBinding
import com.nishant.smartattendance.feature.auth.LoginActivity
import kotlinx.coroutines.launch

class StudentProfileFragment : Fragment() {

    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()

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
                    binding.tvProfileSrn.text = "SRN: N/A - Contact admin"
                    binding.tvProfileCourse.text = ""
                    binding.tvProfileSection.text = ""
                    return@launch
                }

                binding.tvProfileName.text = student.name
                binding.tvProfileSrn.text = "SRN: ${student.srn}"
                binding.tvProfileEmail.text = "Email: ${student.email}"
                binding.tvProfileCourse.text = "Course: ${student.courseId}"
                binding.tvProfileSection.text = "Section: ${student.section} | Roll No: ${student.rollNo}"

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