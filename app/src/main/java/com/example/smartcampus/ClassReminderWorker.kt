package com.example.smartcampus

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ClassReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ClassReminderWorker", "Worker is running...")
        try {
            // --- THIS IS THE FIX ---
            val timetableDao = SmartCampusApp.getDatabase().timetableDao()
            // -----------------------

            val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
            val todaySchedule = timetableDao.getTimetableForDay(today)

            if (todaySchedule.isEmpty()) {
                Log.d("ClassReminderWorker", "No classes scheduled for today. Worker finished.")
                return Result.success()
            }

            val now = Calendar.getInstance()

            for (classEntry in todaySchedule) {
                val classTime = parseTime(classEntry.timeSlot) ?: continue
                val classStartTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, classTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, classTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }

                val diffMillis = classStartTime.timeInMillis - now.timeInMillis
                val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)

                if (diffMinutes in 10..25) {
                    Log.d("ClassReminderWorker", "Found upcoming class: ${classEntry.subjectName}. Sending notification.")
                    NotificationHelper(applicationContext).sendClassReminderNotification(
                        classEntry.subjectName,
                        classEntry.location,
                        diffMinutes.toInt()
                    )
                    return Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e("ClassReminderWorker", "Error in worker: ${e.message}")
            return Result.failure()
        }

        Log.d("ClassReminderWorker", "No upcoming classes found in the next 25 mins. Worker finished.")
        return Result.success()
    }

    private fun parseTime(timeString: String): Calendar? {
        val startTimeString = timeString.split("-")[0].trim()
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return try {
            val date = format.parse(startTimeString)
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            Log.e("ClassReminderWorker", "Failed to parse time: $timeString")
            null
        }
    }
}