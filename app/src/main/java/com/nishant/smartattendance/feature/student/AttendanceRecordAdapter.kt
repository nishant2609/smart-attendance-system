package com.nishant.smartattendance.feature.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemAttendanceRecordBinding
import com.nishant.smartattendance.domain.model.AttendanceRecord

class AttendanceRecordAdapter(
    private val records: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceRecordAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAttendanceRecordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.binding.tvRecordCourse.text = "${record.courseId} - Section ${record.section}"
        holder.binding.tvRecordSubject.text = record.subject
        holder.binding.tvRecordDate.text = record.date

        if (record.status == "present") {
            holder.binding.tvRecordStatus.text = "Present"
            holder.binding.tvRecordStatus.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
            holder.binding.tvRecordStatus.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            )
        } else {
            holder.binding.tvRecordStatus.text = "Absent"
            holder.binding.tvRecordStatus.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
            holder.binding.tvRecordStatus.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_red_light)
            )
        }
    }

    override fun getItemCount() = records.size
}