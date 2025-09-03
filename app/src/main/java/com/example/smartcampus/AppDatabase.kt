package com.example.smartcampus

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserProfile::class, TimetableEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun timetableDao(): TimetableDao
}