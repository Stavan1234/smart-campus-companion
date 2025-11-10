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

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

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
        // --- CORRECTED DATABASE ACCESS ---
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

    private fun checkAndDisplaySmartAlert(schedule: List<TimetableEntry>) {
        var alertMessage: String? = null
        for (i in 0 until schedule.size - 1) {
            val currentClass = schedule[i]
            val nextClass = schedule[i + 1]
            if (currentClass.location != nextClass.location) {
                alertMessage = "Heads up! Your class in ${nextClass.location} is right after one in ${currentClass.location}."
                break
            }
        }

        // --- CORRECTED VIEW ACCESS ---
        val aiAlertTextView = binding.aiAlertTextView
        if (alertMessage != null) {
            aiAlertTextView.text = alertMessage
        } else {
            aiAlertTextView.text = "Your schedule looks clear today! ✨ No back-to-back classes in different locations."
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