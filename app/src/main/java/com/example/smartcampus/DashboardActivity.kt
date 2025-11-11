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
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    // This is our "ML Model" Knowledge Base
    private val locationDistanceMap = mapOf(
        "AX411" to "AX408" to 30,
        "AX411" to "AX510" to 80,
        "AX408" to "AX510" to 90,
        "AX411" to "CANTEEN" to 200,
        "AX408" to "CANTEEN" to 210,
        "AX510" to "CANTEEN" to 280,
        "AX411" to "LIBRARY" to 150,
        "AX408" to "LIBRARY" to 160,
        "AX510" to "LIBRARY" to 230,
        "LIBRARY" to "CANTEEN" to 100
    )

    private fun getDistance(loc1: String, loc2: String): Int {
        if (loc1 == loc2) return 0
        return locationDistanceMap[loc1 to loc2]
            ?: locationDistanceMap[loc2 to loc1]
            ?: 50
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

        setupClickListeners() // This function is now updated
        setupEventsRecyclerView()
        loadDashboardData()
        startAnimations()
        askNotificationPermission()
    }

    // *** THIS FUNCTION IS NOW UPDATED ***
    private fun setupClickListeners() {

        // --- Main Smart Cards ---
        binding.mapCard.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.timetableCard.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        binding.viewScheduleLink.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        // --- Quick Action Buttons ---
        binding.btnActionTimetable.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        binding.btnActionCanteen.setOnClickListener {
            startActivity(Intent(this, CanteenMenuActivity::class.java))
        }

        binding.btnActionMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnActionChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        // --- Events ---
        binding.viewAllEventsButton.setOnClickListener {
            startActivity(Intent(this, AllEventsActivity::class.java))
        }

        // --- Other Cards & FABs ---
        binding.canteenMenuCard.setOnClickListener {
            startActivity(Intent(this, CanteenMenuActivity::class.java))
        }

        // This card is now at the bottom, but the listener is the same
        binding.contactSupportCard.setOnClickListener {
            binding.contactSupportCard.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            Toast.makeText(this, "Contact info displayed", Toast.LENGTH_SHORT).show()
        }

        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
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
        checkAndDisplaySmartAlert(scheduleList)
    }

    private fun parseTimeSlot(timeSlot: String): Pair<Int, Int>? {
        return try {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            val parts = timeSlot.split("-").map { it.trim() }
            val startDate = format.parse(parts[0])
            val endDate = format.parse(parts[1])

            val cal = Calendar.getInstance()
            cal.time = startDate
            val startTotalMinutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)

            cal.time = endDate
            val endTotalMinutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)

            Pair(startTotalMinutes, endTotalMinutes)
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Failed to parse timeSlot: $timeSlot", e)
            null
        }
    }

    private fun getLatenessPrediction(timeGapInMinutes: Int, distanceInMeters: Int): String {
        val requiredWalkTime = distanceInMeters / 84.0
        val totalRequiredTime = requiredWalkTime + 1

        return when {
            totalRequiredTime > timeGapInMinutes ->
                "**High Risk:** You only have $timeGapInMinutes min to travel ${distanceInMeters}m. You will be late!"
            totalRequiredTime > (timeGapInMinutes * 0.7) ->
                "**Heads Up:** You have $timeGapInMinutes min to travel ${distanceInMeters}m. You'll need to walk fast!"
            else ->
                "Your schedule looks clear today! ✨ No back-to-back classes."
        }
    }

    // *** THIS IS THE UPDATED UI FUNCTION ***
    private fun checkAndDisplaySmartAlert(schedule: List<TimetableEntry>) {
        var smartAlertMessage = "Your schedule looks clear today! ✨ No back-to-back classes."

        for (i in 0 until schedule.size - 1) {
            val currentClass = schedule[i]
            val nextClass = schedule[i + 1]

            val currentTimes = parseTimeSlot(currentClass.timeSlot)
            val nextTimes = parseTimeSlot(nextClass.timeSlot)

            if (currentTimes != null && nextTimes != null) {
                val currentEndTime = currentTimes.second
                val nextStartTime = nextTimes.first
                val timeGapInMinutes = nextStartTime - currentEndTime
                val distanceInMeters = getDistance(currentClass.location, nextClass.location)

                if (timeGapInMinutes <= 15) {
                    val prediction = getLatenessPrediction(timeGapInMinutes, distanceInMeters)
                    if (!prediction.startsWith("Your schedule")) {
                        smartAlertMessage = prediction
                        break
                    }
                }
            }
        }

        binding.aiAlertTextView.text = smartAlertMessage

        // *** UI FIX: Set TEXT color, not card color ***
        val colorRes = when {
            smartAlertMessage.startsWith("**High Risk:**") -> R.color.error // This is a strong red
            smartAlertMessage.startsWith("**Heads Up:**") -> R.color.primary_color // This is your app's blue
            else -> R.color.text_secondary // This is a standard gray
        }

        binding.aiAlertTextView.setTextColor(ContextCompat.getColor(this, colorRes))
        // Keep the card white
        binding.aiAlertCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
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