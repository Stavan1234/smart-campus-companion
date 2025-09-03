package com.example.smartcampus

import android.app.Application
import androidx.room.Room
import com.example.smartcampus.AppDatabase

class SmartCampusApp : Application() {

    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        // This creates the actual database on the phone when the app first starts
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "smart-campus-db"
        ).build()
    }
}