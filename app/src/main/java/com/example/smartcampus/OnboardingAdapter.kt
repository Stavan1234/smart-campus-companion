package com.example.smartcampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val items: List<OnboardingItem>,
    private val onGetStartedClick: () -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.imgOnboarding)
        private val title: TextView = view.findViewById(R.id.txtTitle)
        private val description: TextView = view.findViewById(R.id.txtDescription)
        private val btnGetStarted: Button = view.findViewById(R.id.btnGetStarted)

        fun bind(item: OnboardingItem, isLast: Boolean) {
            image.setImageResource(item.imageRes)
            title.text = item.title
            description.text = item.description

            // Button only visible on last page
            btnGetStarted.visibility = if (isLast) View.VISIBLE else View.GONE
            btnGetStarted.setOnClickListener { onGetStartedClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position], position == items.lastIndex)
    }

    override fun getItemCount(): Int = items.size
}
