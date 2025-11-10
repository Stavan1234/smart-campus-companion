package com.example.smartcampus
import com.google.firebase.firestore.Exclude
data class Event(
    @get:Exclude var id: String = "",
    val title: String = "",
    val date: String = "",
    val imageUrl: String = "",
    val category: String = ""
)