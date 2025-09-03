package com.example.smartcampus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartcampus.TimetableEntry
import com.example.smartcampus.databinding.ActivityDashboardBinding
import com.example.smartcampus.databinding.ItemScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        setupEventsRecyclerView()
        loadDashboardData()
    }

    private fun setupEventsRecyclerView() {
        binding.eventsRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadDashboardData() {
        val userDao = SmartCampusApp.database.userDao()
        val timetableDao = SmartCampusApp.database.timetableDao()

        // Use a single coroutine to fetch all data
        lifecycleScope.launch(Dispatchers.IO) {
            // --- Fetch User Profile from local DB ---
            val user = userDao.getUser()
            withContext(Dispatchers.Main) {
                binding.userNameTextView.text = user?.userName ?: "Student"
            }

            // --- Fetch Timetable from local DB ---
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            val dayOfWeek = sdf.format(Date())
            val todaySchedule = timetableDao.getTimetableForDay(dayOfWeek)
            withContext(Dispatchers.Main) {
                updateScheduleUI(todaySchedule)
            }

            // --- Fetch Events from Firebase ---
            try {
                val snapshot = firestore.collection("events")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(4)
                    .get()
                    .await()

                val eventList = snapshot.toObjects(Event::class.java)

                withContext(Dispatchers.Main) {
                    binding.eventsRecyclerView.adapter = EventsAdapter(eventList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("DashboardActivity", "Error fetching events", e)
                }
            }
        }
    }

    private fun updateScheduleUI(scheduleList: List<TimetableEntry>) {
        binding.scheduleContainer.removeAllViews()

        if (scheduleList.isEmpty()) {
            val noClassesTextView = TextView(this)
            noClassesTextView.text = "No classes scheduled for today."
            binding.scheduleContainer.addView(noClassesTextView)
        } else {
            val inflater = LayoutInflater.from(this)
            for (entry in scheduleList) {
                val scheduleItemBinding = ItemScheduleBinding.inflate(inflater, binding.scheduleContainer, false)
                scheduleItemBinding.subjectTextView.text = entry.subjectName
                scheduleItemBinding.timeTextView.text = "${entry.timeSlot} • ${entry.location}"
                binding.scheduleContainer.addView(scheduleItemBinding.root)
            }
        }
    }
}