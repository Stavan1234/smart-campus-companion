package com.example.smartcampus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile_table")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1, // We only have one user, so the ID is fixed
    val userName: String,
    val photoUrl: String
)