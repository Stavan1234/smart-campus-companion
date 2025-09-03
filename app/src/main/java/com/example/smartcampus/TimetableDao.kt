package com.example.smartcampus

import androidx.room.*

@Dao
interface TimetableDao {
    @Insert
    suspend fun insertAll(timetable: List<TimetableEntry>)

    @Query("SELECT * FROM timetable_table WHERE dayOfWeek = :day")
    fun getTimetableForDay(day: String): List<TimetableEntry>

    @Query("DELETE FROM timetable_table")
    suspend fun clearTable()
}