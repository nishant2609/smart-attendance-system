package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemStudentBinding
import com.nishant.smartattendance.domain.model.Student

class StudentAdapter(
    private val students: List<Student>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.binding.tvStudentName.text = student.name
        holder.binding.tvStudentSrn.text = "SRN: ${student.srn}"
        holder.binding.tvStudentRoll.text = "Roll No: ${student.rollNo}"
        holder.binding.tvFaceStatus.text = if (student.faceRegistered) "Face ✓" else "No Face"
        holder.binding.tvFaceStatus.setTextColor(
            if (student.faceRegistered)
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            else
                holder.itemView.context.getColor(android.R.color.holo_red_light)
        )
    }

    override fun getItemCount() = students.size
}