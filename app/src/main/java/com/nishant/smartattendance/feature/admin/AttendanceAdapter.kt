package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemAttendanceBinding
import com.nishant.smartattendance.domain.model.Student

class AttendanceAdapter(
    private val students: List<Student>,
    private val attendanceMap: MutableMap<String, Boolean>
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
        holder.binding.tvStudentName.text = student.name
        holder.binding.tvStudentSrn.text = "SRN: ${student.srn}"

        // Set initial state without triggering listener
        holder.binding.switchPresent.setOnCheckedChangeListener(null)
        holder.binding.switchPresent.isChecked = attendanceMap[student.srn] ?: false

        holder.binding.switchPresent.setOnCheckedChangeListener { _, isChecked ->
            attendanceMap[student.srn] = isChecked
        }
    }

    override fun getItemCount() = students.size
}