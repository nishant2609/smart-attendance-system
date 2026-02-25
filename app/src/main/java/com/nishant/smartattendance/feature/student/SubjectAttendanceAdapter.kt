package com.nishant.smartattendance.feature.student

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.R
import java.text.SimpleDateFormat
import java.util.Locale

class SubjectAttendanceAdapter(
    private val items: List<SubjectAttendanceSummary>
) : RecyclerView.Adapter<SubjectAttendanceAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubject: TextView   = view.findViewById(R.id.tvSubjectName)
        val tvStats: TextView     = view.findViewById(R.id.tvSubjectStats)
        val tvPct: TextView       = view.findViewById(R.id.tvSubjectPct)
        val progress: ProgressBar = view.findViewById(R.id.progressSubject)
        val tvLastDate: TextView  = view.findViewById(R.id.tvLastDate)
        val tvWarning: TextView   = view.findViewById(R.id.tvSubjectWarning)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_attendance, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val pct = item.percentage

        holder.tvSubject.text = item.courseName
        holder.tvStats.text = "${item.present} / ${item.total} classes attended"
        holder.tvPct.text = "%.1f%%".format(pct)
        holder.progress.progress = pct.toInt()

        val color = when {
            pct >= 75f -> 0xFF2E7D32.toInt()
            pct >= 60f -> 0xFFF57C00.toInt()
            else       -> 0xFFD32F2F.toInt()
        }
        holder.tvPct.setTextColor(color)
        holder.progress.progressTintList = ColorStateList.valueOf(color)

        if (item.lastDate.isNotEmpty()) {
            holder.tvLastDate.visibility = View.VISIBLE
            holder.tvLastDate.text = "Last: ${formatDate(item.lastDate)}"
        } else {
            holder.tvLastDate.visibility = View.GONE
        }

        when {
            pct < 60f -> {
                holder.tvWarning.visibility = View.VISIBLE
                holder.tvWarning.text = "⚠ Critical — below 60%"
                holder.tvWarning.setTextColor(0xFFD32F2F.toInt())
                holder.tvWarning.setBackgroundColor(0xFFFFEBEE.toInt())
            }
            pct < 75f -> {
                holder.tvWarning.visibility = View.VISIBLE
                holder.tvWarning.text = "⚠ Below 75% threshold"
                holder.tvWarning.setTextColor(0xFFF57C00.toInt())
                holder.tvWarning.setBackgroundColor(0xFFFFF8E1.toInt())
            }
            else -> holder.tvWarning.visibility = View.GONE
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val from = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val to = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            to.format(from.parse(dateStr)!!)
        } catch (e: Exception) { dateStr }
    }

    override fun getItemCount() = items.size
}
