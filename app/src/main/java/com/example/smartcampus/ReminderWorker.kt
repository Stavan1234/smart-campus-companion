package com.example.smartcampus

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "ReminderWorker_Debug"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker is running...")
        val firestore = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.d(TAG, "Worker stopping: User is not logged in.")
            return Result.success() // Not a failure, just stop.
        }

        try {
            Log.d(TAG, "Querying Firestore for due reminders for user: ${user.uid}")
            val now = Date()
            val querySnapshot = firestore.collection("reminders")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("isSent", false)
                .whereLessThan("dueDate", now)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.d(TAG, "Query successful, but found 0 due reminders.")
                return Result.success()
            }

            Log.d(TAG, "SUCCESS: Found ${querySnapshot.size()} reminders to send.")
            val notificationHelper = NotificationHelper(applicationContext)

            for (document in querySnapshot.documents) {
                val reminderText = document.getString("text") ?: "You have a reminder."
                Log.d(TAG, "Sending notification for: '$reminderText'")

                notificationHelper.sendGenericReminderNotification(
                    "Smart Campus Reminder",
                    "🔔 $reminderText"
                )

                // Mark the reminder as sent
                firestore.collection("reminders").document(document.id).update("isSent", true)
                Log.d(TAG, "Marked reminder ${document.id} as sent.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Worker failed with an exception.", e)
            return Result.retry()
        }

        return Result.success()
    }
}