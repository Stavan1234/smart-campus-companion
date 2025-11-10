package com.example.smartcampus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcampus.databinding.ItemScheduleCardBinding

class ScheduleAdapter(
    private val onReminderClick: (TimetableEntry) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private var scheduleList = listOf<TimetableEntry>()

    fun updateSchedule(newSchedule: List<TimetableEntry>) {
        scheduleList = newSchedule
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }

    override fun getItemCount(): Int = scheduleList.size

    inner class ScheduleViewHolder(
        private val binding: ItemScheduleCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timetableEntry: TimetableEntry) {
            binding.apply {
                subjectNameText.text = timetableEntry.subjectName
                timeSlotText.text = timetableEntry.timeSlot
                locationText.text = timetableEntry.location
                
                setReminderButton.setOnClickListener {
                    onReminderClick(timetableEntry)
                }
                
                // Add subtle animation to cards
                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(adapterPosition * 100L)
                    .start()
            }
        }
    }
}
