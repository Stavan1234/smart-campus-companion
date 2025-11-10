package com.example.smartcampus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcampus.databinding.ActivityTimetableBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TimetableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimetableBinding
    private lateinit var timetableDao: TimetableDao
    private var selectedDay = "Monday"
    private val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDatabase()
        setupDayChips()
        setupScheduleRecyclerView()
        loadScheduleForDay(selectedDay)
        createNotificationChannel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Weekly Schedule"
    }

    private fun setupDatabase() {
        timetableDao = SmartCampusApp.getDatabase().timetableDao()
    }

    private fun setupDayChips() {
        binding.dayChipGroup.removeAllViews()
        
        daysOfWeek.forEach { day ->
            val chip = Chip(this).apply {
                text = day
                isCheckable = true
                isChecked = day == selectedDay
                setOnClickListener {
                    selectedDay = day
                    loadScheduleForDay(day)
                    updateChipSelection()
                }
            }
            binding.dayChipGroup.addView(chip)
        }
    }

    private fun updateChipSelection() {
        for (i in 0 until binding.dayChipGroup.childCount) {
            val chip = binding.dayChipGroup.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == selectedDay
        }
    }

    private fun setupScheduleRecyclerView() {
        scheduleAdapter = ScheduleAdapter { timetableEntry ->
            setReminderForClass(timetableEntry)
        }
        binding.scheduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TimetableActivity)
            adapter = scheduleAdapter
        }
    }

    private fun loadScheduleForDay(day: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val schedule = timetableDao.getTimetableForDay(day)
            withContext(Dispatchers.Main) {
                if (schedule.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    scheduleAdapter.updateSchedule(schedule)
                    // Add smooth animation for schedule loading
                    binding.scheduleRecyclerView.startAnimation(
                        android.view.animation.AnimationUtils.loadAnimation(this@TimetableActivity, R.anim.slide_in_right)
                    )
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.scheduleRecyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.scheduleRecyclerView.visibility = View.VISIBLE
    }

    private fun setReminderForClass(timetableEntry: TimetableEntry) {
        val reminderTime = calculateReminderTime(timetableEntry.timeSlot)
        if (reminderTime != null) {
            scheduleNotification(timetableEntry, reminderTime)
            Toast.makeText(this, "Reminder set for ${timetableEntry.subjectName} at ${timetableEntry.timeSlot}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to set reminder for this time slot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateReminderTime(timeSlot: String): Long? {
        return try {
            // Parse time slot (e.g., "09:00-10:00" or "9:00 AM - 10:00 AM")
            val timePattern = Regex("(\\d{1,2}):?(\\d{2})?\\s*(AM|PM)?")
            val match = timePattern.find(timeSlot)
            
            if (match != null) {
                val hour = match.groupValues[1].toInt()
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val amPm = match.groupValues[3]
                
                var adjustedHour = hour
                if (amPm.equals("PM", ignoreCase = true) && hour != 12) {
                    adjustedHour += 12
                } else if (amPm.equals("AM", ignoreCase = true) && hour == 12) {
                    adjustedHour = 0
                }
                
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                
                // Set reminder 15 minutes before class
                calendar.add(Calendar.MINUTE, -15)
                
                // If the time has already passed today, set for tomorrow
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                calendar.timeInMillis
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scheduleNotification(timetableEntry: TimetableEntry, reminderTime: Long) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ClassReminderReceiver::class.java).apply {
                putExtra("subject", timetableEntry.subjectName)
                putExtra("time", timetableEntry.timeSlot)
                putExtra("location", timetableEntry.location)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                timetableEntry.subjectName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Check if we can schedule exact alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied for setting reminders", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to set reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "class_reminders",
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming classes"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
