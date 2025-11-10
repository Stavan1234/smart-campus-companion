package com.example.smartcampus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.smartcampus.databinding.ActivityDashboardBinding
import com.example.smartcampus.databinding.ItemScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar // <-- *** FIX: ADDED MISSING IMPORT ***

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    // *** 1. THIS IS THE NEW "DISTANCE MATRIX" (THE MODEL'S KNOWLEDGE) ***
    // It now uses your REAL room names from Firebase.
    private val locationDistanceMap = mapOf(
        // AX 4th Floor
        "AX411" to "AX408" to 30, // 30 meters (down the hall)

        // AX 4th Floor to 5th Floor
        "AX411" to "AX510" to 80, // 80 meters (up one floor)
        "AX408" to "AX510" to 90, // 90 meters (up one floor, opposite end)

        // AX Block to Canteen (Assuming Canteen is Ground Floor)
        "AX411" to "CANTEEN" to 200,
        "AX408" to "CANTEEN" to 210,
        "AX510" to "CANTEEN" to 280,

        // AX Block to Library
        "AX411" to "LIBRARY" to 150,
        "AX408" to "LIBRARY" to 160,
        "AX510" to "LIBRARY" to 230,

        // Library to Canteen
        "LIBRARY" to "CANTEEN" to 100
    )

    // Helper function to safely get the distance
    private fun getDistance(loc1: String, loc2: String): Int {
        if (loc1 == loc2) return 0
        // Check both "A to B" and "B to A"
        return locationDistanceMap[loc1 to loc2]
            ?: locationDistanceMap[loc2 to loc1]
            ?: 50 // Default to 50m if location is unknown
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are disabled. You may miss important reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        setupClickListeners()
        setupEventsRecyclerView()
        loadDashboardData()
        startAnimations()
        askNotificationPermission()
    }

    private fun setupClickListeners() {
        binding.mapCard.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.viewAllEventsButton.setOnClickListener {
            startActivity(Intent(this, AllEventsActivity::class.java))
        }

        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        binding.timetableCard.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        binding.viewScheduleLink.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        binding.contactSupportCard.setOnClickListener {
            // Add haptic feedback for better UX
            binding.contactSupportCard.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            Toast.makeText(this, "Contact info displayed above", Toast.LENGTH_SHORT).show()
        }

        binding.canteenMenuCard.setOnClickListener {
            startActivity(Intent(this, CanteenMenuActivity::class.java))
        }

        binding.avatarImageView.setOnClickListener { view ->
            showProfileMenu(view)
        }
    }

    private fun showProfileMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    firebaseAuth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    Toast.makeText(this, "Profile screen coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupEventsRecyclerView() {
        binding.eventsRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadDashboardData() {
        val userDao = SmartCampusApp.getDatabase().userDao()
        val timetableDao = SmartCampusApp.getDatabase().timetableDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            withContext(Dispatchers.Main) {
                binding.userNameTextView.text = user?.userName ?: "Student"

                if (user != null && user.photoUrl.isNotBlank()) {
                    Glide.with(this@DashboardActivity)
                        .load(user.photoUrl)
                        .circleCrop()
                        .into(binding.avatarImageView)
                }
            }

            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            val dayOfWeek = sdf.format(Date())
            val todaySchedule = timetableDao.getTimetableForDay(dayOfWeek)
            withContext(Dispatchers.Main) {
                updateScheduleUI(todaySchedule)
            }

            try {
                val snapshot = firestore.collection("events")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(4)
                    .get()
                    .await()
                val eventList = snapshot.documents.mapNotNull { document ->
                    document.toObject(Event::class.java)?.apply { id = document.id }
                }
                withContext(Dispatchers.Main) {
                    binding.eventsRecyclerView.adapter = EventsAdapter(eventList)
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error fetching events", e)
            }
        }
    }

    private fun updateScheduleUI(scheduleList: List<TimetableEntry>) {
        binding.scheduleContainer.removeAllViews()

        if (scheduleList.isEmpty()) {
            val noClassesTextView = TextView(this)
            noClassesTextView.text = getString(R.string.no_classes_today)
            binding.scheduleContainer.addView(noClassesTextView)
        } else {
            val inflater = LayoutInflater.from(this)
            for (entry in scheduleList) {
                val scheduleItemBinding = ItemScheduleBinding.inflate(inflater, binding.scheduleContainer, false)
                scheduleItemBinding.subjectTextView.text = entry.subjectName
                scheduleItemBinding.timeTextView.text = getString(R.string.schedule_item_details, entry.timeSlot, entry.location)
                binding.scheduleContainer.addView(scheduleItemBinding.root)
            }
        }
        // This will now call our NEW, smart alert function
        checkAndDisplaySmartAlert(scheduleList)
    }

    // *** 2. THIS IS THE NEW "TIME PARSER" (THE "FEATURE ENGINEERING") ***
    // This function now correctly parses "8:45 AM - 10:45 AM".
    /**
     * Parses a time string like "8:45 AM - 10:45 AM" or "1:30 PM - 2:30 PM".
     * Returns the start and end time in minutes-from-midnight.
     * e.g., "1:30 PM - 2:30 PM" -> Pair(810, 870)
     */
    private fun parseTimeSlot(timeSlot: String): Pair<Int, Int>? {
        return try {
            // Input format: "h:mm a" (e.g., "8:45 AM")
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())

            val parts = timeSlot.split("-").map { it.trim() }
            val startTimeStr = parts[0]
            val endTimeStr = parts[1]

            val startDate = format.parse(startTimeStr)
            val endDate = format.parse(endTimeStr)

            // Convert parsed date to minutes-from-midnight
            val cal = Calendar.getInstance()

            cal.time = startDate
            val startTotalMinutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)

            cal.time = endDate
            val endTotalMinutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)

            // *** FIX: Correct return type ***
            Pair(startTotalMinutes, endTotalMinutes)
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Failed to parse timeSlot: $timeSlot", e)
            null // Return null if parsing fails
        }
    }

    // *** 3. THIS IS OUR "ML MODEL" ***
    // It takes the features (time and distance) and returns a prediction.
    /**
     * @param timeGapInMinutes The time (in minutes) between classes.
     * @param distanceInMeters The distance (in meters) to travel.
     * @return A prediction string (our "classification").
     */
    private fun getLatenessPrediction(timeGapInMinutes: Int, distanceInMeters: Int): String {
        // Average walking speed is ~1.4 meters/second (or 84 meters/minute)
        val requiredWalkTime = distanceInMeters / 84.0

        // Add 1 minute for "buffer" (getting out of class, packing up)
        val totalRequiredTime = requiredWalkTime + 1

        return when {
            // RULE 1: If total required time is > available time, high risk.
            totalRequiredTime > timeGapInMinutes ->
                "**High Risk:** You only have $timeGapInMinutes min to travel ${distanceInMeters}m. You will be late!"

            // RULE 2: If total time takes > 70% of gap, medium risk.
            totalRequiredTime > (timeGapInMinutes * 0.7) ->
                "**Heads Up:** You have $timeGapInMinutes min to travel ${distanceInMeters}m. You'll need to walk fast!"

            // RULE 3: Otherwise, low risk.
            else ->
                "Your schedule looks clear today! ✨ No back-to-back classes."
        }
    }

    // *** 4. THIS IS THE NEW "PREDICTION" FUNCTION ***
    // (This replaces your old, simple checkAndDisplaySmartAlert)
    private fun checkAndDisplaySmartAlert(schedule: List<TimetableEntry>) {
        var smartAlertMessage = "Your schedule looks clear today! ✨ No back-to-back classes."

        // Loop through the schedule, stopping before the last class
        for (i in 0 until schedule.size - 1) {
            val currentClass = schedule[i]
            val nextClass = schedule[i + 1]

            // 1. Feature Engineering: Parse the times
            val currentTimes = parseTimeSlot(currentClass.timeSlot)
            val nextTimes = parseTimeSlot(nextClass.timeSlot)

            // If both times are valid, let's make a prediction
            if (currentTimes != null && nextTimes != null) {
                // *** FIX: No more 'times' reference ***
                val currentEndTime = currentTimes.second
                val nextStartTime = nextTimes.first

                // 2. Feature Engineering: Get the features
                val timeGapInMinutes = nextStartTime - currentEndTime
                val distanceInMeters = getDistance(currentClass.location, nextClass.location)

                // 3. Run the "ML Model"
                // We only care about immediate back-to-back classes (gap <= 15 min)
                if (timeGapInMinutes <= 15) {
                    val prediction = getLatenessPrediction(timeGapInMinutes, distanceInMeters)

                    // If the prediction is NOT the default "clear" message, use it
                    if (!prediction.startsWith("Your schedule")) {
                        smartAlertMessage = prediction
                        break // We found the most important alert, so we can stop
                    }
                }
            }
        }

        // 4. Display the result
        binding.aiAlertTextView.text = smartAlertMessage

        // 5. Bonus: Change the card color based on risk
        val colorRes = when {
            smartAlertMessage.startsWith("**High Risk:**") -> R.color.warning
            smartAlertMessage.startsWith("**Heads Up:**") -> R.color.primary_light
            else -> android.R.color.white // Use a default white
        }

        try {
            binding.aiAlertCard.setCardBackgroundColor(ContextCompat.getColor(this, colorRes))
        } catch (e: Exception) {
            binding.aiAlertCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }


    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startAnimations() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.mapCard.startAnimation(slideUp)
        binding.aiAlertCard.postDelayed({ binding.aiAlertCard.startAnimation(slideUp) }, 100)
        (binding.scheduleContainer.parent as View).postDelayed({ (binding.scheduleContainer.parent as View).startAnimation(slideUp) }, 200)
        binding.eventsRecyclerView.postDelayed({ binding.eventsRecyclerView.startAnimation(slideUp) }, 300)
    }
}