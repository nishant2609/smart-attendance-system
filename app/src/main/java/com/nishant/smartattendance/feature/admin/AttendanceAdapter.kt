package com.nishant.smartattendance.feature.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemAttendanceBinding
import com.nishant.smartattendance.domain.model.Student

class AttendanceAdapter(
    private val students: List<Student>,
    private val attendanceMap: MutableMap<String, Boolean?>,
    private val isEditable: Boolean = true
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val student = students[position]
        val binding = holder.binding

        // Basic info
        binding.tvStudentName.text = student.name
        binding.tvStudentSrn.text = student.srn
        binding.tvAttendanceInitial.text = student.name.firstOrNull()?.uppercase() ?: "?"

        // Restore current state — null = neither selected
        val currentStatus: Boolean? = attendanceMap[student.srn]
        updateButtonStates(binding, currentStatus)

        // Disable buttons for past-date read-only mode
        binding.btnPresent.isEnabled = isEditable
        binding.btnAbsent.isEnabled = isEditable

        if (isEditable) {
            binding.btnPresent.setOnClickListener {
                val newStatus = if (attendanceMap[student.srn] == true) null else true
                attendanceMap[student.srn] = newStatus
                updateButtonStates(binding, newStatus)
            }

            binding.btnAbsent.setOnClickListener {
                val newStatus = if (attendanceMap[student.srn] == false) null else false
                attendanceMap[student.srn] = newStatus
                updateButtonStates(binding, newStatus)
            }
        }
    }

    private fun updateButtonStates(binding: ItemAttendanceBinding, status: Boolean?) {
        binding.btnPresent.isSelected = (status == true)
        binding.btnAbsent.isSelected  = (status == false)

        // Text color: white when selected, muted grey when not
        binding.btnPresent.setTextColor(
            if (status == true) Color.WHITE else Color.parseColor("#9FA8DA")
        )
        binding.btnAbsent.setTextColor(
            if (status == false) Color.WHITE else Color.parseColor("#9FA8DA")
        )
    }

    override fun getItemCount() = students.size
}