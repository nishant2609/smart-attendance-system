package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemCourseCardBinding
import com.nishant.smartattendance.domain.model.Course

class CourseAdapter(
    private val courses: List<Course>
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(val binding: ItemCourseCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.binding.tvCourseName.text = course.name
        holder.binding.tvCourseFullName.text = course.fullName
        holder.binding.tvSectionCount.text = "${course.sections.size} Sections"
    }

    override fun getItemCount() = courses.size
}