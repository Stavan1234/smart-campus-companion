package com.example.smartcampus

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val subject = intent.getStringExtra("subject") ?: "Class"
            val time = intent.getStringExtra("time") ?: ""
            val location = intent.getStringExtra("location") ?: ""

            val notification = NotificationCompat.Builder(context, "class_reminders")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Class Reminder")
                .setContentText("$subject at $time in $location")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Your class '$subject' is starting in 15 minutes at $location"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(subject.hashCode(), notification)
            }
        } catch (e: Exception) {
            // Handle any notification errors gracefully
            android.util.Log.e("ClassReminderReceiver", "Error showing notification", e)
        }
    }
}
