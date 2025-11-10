package com.example.smartcampus

import com.google.firebase.Timestamp

data class EventDetail(
    // We add default values to make the class robust against missing data in Firestore
    val title: String = "",
    val category: String = "",
    val date: String = "",
    val location: String = "",
    val venue: String = "", // e.g., "Main Auditorium"
    val description: String = "", // This will be our full, detailed description
    val imageUrl: String = "",
    val videoUrl: String = "", // Optional: for a YouTube or other video link
    val timestamp: Timestamp = Timestamp.now(),
    val organizer: String = "",
    val tags: List<String> = listOf()
)