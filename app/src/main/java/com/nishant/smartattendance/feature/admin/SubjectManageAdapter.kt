package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemSubjectManageBinding

class SubjectManageAdapter(
    private val subjects: MutableList<String>,
    private val onDeleteClick: (String, Int) -> Unit
) : RecyclerView.Adapter<SubjectManageAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSubjectManageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectManageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subject = subjects[position]
        holder.binding.tvSubjectName.text = subject
        holder.binding.btnDeleteSubject.setOnClickListener {
            onDeleteClick(subject, position)
        }
    }

    override fun getItemCount() = subjects.size

    fun addSubject(subject: String) {
        subjects.add(subject)
        notifyItemInserted(subjects.size - 1)
    }

    fun removeSubject(position: Int) {
        subjects.removeAt(position)
        notifyItemRemoved(position)
    }
}