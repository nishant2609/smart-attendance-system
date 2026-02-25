package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.R

data class LowAttendanceAlert(
    val studentName: String,
    val srn: String,
    val subject: String,
    val percentage: Float
)

class LowAttendanceAdapter(
    private val items: List<LowAttendanceAlert>
) : RecyclerView.Adapter<LowAttendanceAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView    = view.findViewById(R.id.tvAlertStudentName)
        val tvSubject: TextView = view.findViewById(R.id.tvAlertSubject)
        val tvPct: TextView     = view.findViewById(R.id.tvAlertPct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_low_attendance_alert, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.studentName
        holder.tvSubject.text = item.subject
        holder.tvPct.text = "%.0f%%".format(item.percentage)
        val color = if (item.percentage < 60f) 0xFFD32F2F.toInt() else 0xFFF57C00.toInt()
        holder.tvPct.setTextColor(color)
    }

    override fun getItemCount() = items.size
}
