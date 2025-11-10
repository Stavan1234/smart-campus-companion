package com.example.smartcampus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Calendar
import java.util.Date

class CampusBotBrain(
    private val firestore: FirebaseFirestore,
    private val onResponse: (response: String, locationName: String?) -> Unit
) {

    fun process(userInput: String) {
        val text = userInput.lowercase().trim()

        when {
            "remind me" in text || "set reminder" in text -> {
                handleReminder(text)
            }
            listOf("hi", "hello", "hey").any { it in text } -> {
                onResponse("Hello! 👋 How can I help you today?", null)
            }
            listOf("library", "canteen", "office", "entrance").any { it in text } -> {
                val keyword = listOf("library", "canteen", "office", "entrance").first { it in text }
                findLocationInfo(keyword, text)
            }
            "thanks" in text || "thank you" in text -> {
                onResponse("You're welcome! Is there anything else I can help with? 😊", null)
            }
            else -> {
                onResponse("Hmm... I can only help with campus locations and timings right now. Try asking something like: 'Where is the library?'", null)
            }
        }
    }

    private fun handleReminder(text: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResponse("Sorry, you need to be logged in to set reminders.", null)
            return
        }

        var reminderText = ""
        var reminderDate: Date? = null

        // Improved text extraction
        val reminderContent = text.substringAfter("to ").substringBefore(" today")
            .substringBefore(" tomorrow")
        if (reminderContent.isNotBlank()) {
            reminderText = reminderContent
        }

        // --- THIS IS THE NEW LOGIC ---
        when {
            "today" in text -> {
                // If the user says "today", just use the current date and time.
                // The background worker will pick it up on its next run.
                reminderDate = Date()
            }
            "tomorrow" in text -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                reminderDate = calendar.time
            }
        }
        // -----------------------------

        if (reminderText.isNotBlank() && reminderDate != null) {
            val reminder = hashMapOf(
                "userId" to user.uid,
                "text" to reminderText.replaceFirstChar { it.uppercase() },
                "dueDate" to reminderDate,
                "isSent" to false
            )

            firestore.collection("reminders").add(reminder)
                .addOnSuccessListener {
                    val timeQualifier = if ("today" in text) "later today" else "tomorrow"
                    onResponse("✅ Got it! I'll remind you to '$reminderText' $timeQualifier.", null)
                }
                .addOnFailureListener {
                    onResponse("⚠️ Sorry, I couldn't save that reminder. Please try again.", null)
                }
        } else {
            onResponse("I can set a reminder for you. Try saying something like: 'Remind me to return my library book tomorrow'.", null)
        }
    }

    private fun findLocationInfo(locationKeyword: String, fullQuery: String) {
        firestore.collection("location")
            .get()
            .addOnSuccessListener { documents ->
                val location = documents.map { it.toObject<MapLocation>() }
                    .find { it.name.contains(locationKeyword, ignoreCase = true) }

                if (location == null) {
                    onResponse("Sorry, I couldn't find any information for '$locationKeyword'.", null)
                    return@addOnSuccessListener
                }

                if ("time" in fullQuery || "timing" in fullQuery || "open" in fullQuery || "close" in fullQuery) {
                    onResponse("Here are the details for the ${location.name}: ${location.details}", null)
                } else {
                    onResponse("I found the ${location.name}! Tap below to see it on the map.", location.name)
                }
            }
            .addOnFailureListener {
                onResponse("⚠️ Oops! I'm having trouble connecting to my knowledge base. Please check your internet connection.", null)
            }
    }
}