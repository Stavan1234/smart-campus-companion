package com.example.smartcampus

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfile)

    @Query("SELECT * FROM user_profile_table WHERE id = 1")
    fun getUser(): UserProfile?
}