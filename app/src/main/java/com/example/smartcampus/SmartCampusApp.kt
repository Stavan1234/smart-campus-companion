package com.example.smartcampus

import android.app.Application
import androidx.room.Room

class SmartCampusApp : Application() {

    // Keep a private instance
    private var _database: AppDatabase? = null

    // Publicly expose the database, creating it only if needed
    val database: AppDatabase
        get() {
            if (_database == null) {
                _database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "smart-campus-db"
                ).build()
            }
            return _database!!
        }

    companion object {
        private lateinit var instance: SmartCampusApp
        // Provide a static way to get the database instance
        fun getDatabase(): AppDatabase = instance.database
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Eagerly initialize the database when the app starts
        _database = database
    }
}