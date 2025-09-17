package com.example.smartcampus.seed

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

object SeedEvents {

    // Run once. If it already ran, it quietly exits.
    suspend fun seedEventsIfNeeded(): Boolean {
        val db = FirebaseFirestore.getInstance()
        val flagRef = db.collection("meta").document("seeds")

        // If flag is set, skip
        val flagSnap = flagRef.get().await()
        if (flagSnap.getBoolean("events_v1") == true) return false

        // Build dummy events
        val events = dummyEvents()

        // Batch write (upsert) with deterministic IDs to avoid duplicates
        val batch = db.batch()
        val col = db.collection("events")
        events.forEach { e ->
            val id = slug("${e["title"]}-${e["date"]}")
            batch.set(col.document(id), e, SetOptions.merge())
        }
        // Mark as seeded
        batch.set(flagRef, mapOf("events_v1" to true), SetOptions.merge())
        batch.commit().await()
        return true
    }

    private fun slug(s: String): String =
        s.lowercase(Locale.US).replace("[^a-z0-9]+".toRegex(), "-").trim('-')

    private fun dummyEvents(): List<Map<String, Any>> {
        // Use stable placeholder images
        fun img(seed: String) = "https://picsum.photos/seed/$seed/800/500"

        val batch1 = listOf(
            mapOf(
                "title" to "Seminar on AI/ML Applications",
                "category" to "Workshops",
                "date" to "September 10, 2025",
                "location" to "Auditorium A",
                "description" to "Industry talk with live demo & hands-on.",
                "imageUrl" to img("aiml"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Annual Sports Meet",
                "category" to "Sports",
                "date" to "October 5, 2025",
                "location" to "Main Stadium",
                "description" to "Track & field, football, volleyball—join your house!",
                "imageUrl" to img("sports"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Hackathon: Build for Campus",
                "category" to "Hackathons",
                "date" to "September 28, 2025",
                "location" to "Innovation Lab",
                "description" to "24-hr hack to solve campus problems.",
                "imageUrl" to img("hack"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Cultural Night",
                "category" to "Cultural",
                "date" to "September 20, 2025",
                "location" to "Open Air Theater",
                "description" to "Music, dance, drama & food stalls.",
                "imageUrl" to img("culture"),
                "timestamp" to System.currentTimeMillis()
            )
        )

        val batch2 = listOf(
            mapOf(
                "title" to "Green Campus Tree Plantation",
                "category" to "Community",
                "date" to "September 16, 2025",
                "location" to "Back Gate Lawn",
                "description" to "Join NSS volunteers in planting 200 saplings.",
                "imageUrl" to img("trees"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Coding Contest: Java Warriors",
                "category" to "Coding",
                "date" to "September 18, 2025",
                "location" to "CS Lab Block C",
                "description" to "Solve 5 coding challenges in 2 hours.",
                "imageUrl" to img("java"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Workshop: Arduino Robotics",
                "category" to "Workshops",
                "date" to "September 22, 2025",
                "location" to "Electronics Lab",
                "description" to "Hands-on Arduino projects: Line followers & bots.",
                "imageUrl" to img("arduino"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Film Club Screening: Interstellar",
                "category" to "Clubs",
                "date" to "September 19, 2025",
                "location" to "Seminar Hall C",
                "description" to "Watch and discuss Nolan’s masterpiece.",
                "imageUrl" to img("filmclub"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Teachers’ Day Celebration",
                "category" to "Cultural",
                "date" to "September 5, 2025",
                "location" to "Main Auditorium",
                "description" to "Student-led cultural program to honor teachers.",
                "imageUrl" to img("teachersday"),
                "timestamp" to System.currentTimeMillis()
            ),
            mapOf(
                "title" to "Startup Showcase",
                "category" to "Entrepreneurship",
                "date" to "October 15, 2025",
                "location" to "Incubation Hub",
                "description" to "Final-year teams present their startups to investors.",
                "imageUrl" to img("startupshow"),
                "timestamp" to System.currentTimeMillis()
            )
        )

        return batch1 + batch2
    }

}
