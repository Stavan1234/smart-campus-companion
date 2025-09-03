package com.example.smartcampus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_table")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectName: String,
    val timeSlot: String,
    val location: String,
    val dayOfWeek: String
)