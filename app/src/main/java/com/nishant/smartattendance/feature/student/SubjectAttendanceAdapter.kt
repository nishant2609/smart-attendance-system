package com.nishant.smartattendance.feature.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemSubjectAttendanceBinding

data class SubjectAttendanceSummary(
    val courseName: String,
    val present: Int,
    val total: Int
) {
    val percentage: Int get() = if (total == 0) 0 else (present * 100) / total
}

class SubjectAttendanceAdapter(
    private val items: List<SubjectAttendanceSummary>
) : RecyclerView.Adapter<SubjectAttendanceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSubjectAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSubjectName.text = item.courseName
        holder.binding.tvSubjectPercent.text = "${item.percentage}%"
        holder.binding.progressAttendance.progress = item.percentage
        holder.binding.tvSubjectCount.text = "${item.present} of ${item.total} classes attended"

        // Color based on percentage
        val color = when {
            item.percentage >= 75 -> android.R.color.holo_green_dark
            item.percentage >= 50 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        holder.binding.tvSubjectPercent.setTextColor(
            holder.itemView.context.getColor(color)
        )
    }

    override fun getItemCount() = items.size
}