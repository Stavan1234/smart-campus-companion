package com.example.smartcampus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartcampus.databinding.ItemEventCardBinding

class EventsAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.binding.eventNameTextView.text = event.title
        holder.binding.eventDateTextView.text = event.date

        // Use Glide to load the image from the URL
        Glide.with(holder.itemView.context)
            .load(event.imageUrl)
            .into(holder.binding.eventImageView)
    }

    override fun getItemCount(): Int {
        return events.size
    }
}