package com.nishant.smartattendance.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nishant.smartattendance.databinding.ItemSubjectBinding

class SubjectListAdapter(
    private val subjects: MutableList<String>
) : RecyclerView.Adapter<SubjectListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSubjectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvSubjectName.text = subjects[position]
    }

    override fun getItemCount() = subjects.size

    fun addSubject(subject: String) {
        subjects.add(subject)
        notifyItemInserted(subjects.size - 1)
    }
}