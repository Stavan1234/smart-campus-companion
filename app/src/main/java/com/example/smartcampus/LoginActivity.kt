package com.example.smartcampus

import android.content.Intent
import android.os.Bundle
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.view.View
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartcampus.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartcampus.seed.SeedEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }
        binding.btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    fetchAndSyncData(firebaseUser.uid)
                    withContext(Dispatchers.IO) { SeedEvents.seedEventsIfNeeded() }
                    scheduleBackgroundWorkers()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // In LoginActivity.kt's scheduleBackgroundWorkers function...

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        val classReminderRequest = PeriodicWorkRequestBuilder<ClassReminderWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueue(classReminderRequest)

        // --- CHANGE 1 HOUR TO 15 MINUTES FOR TESTING ---
        val genericReminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueue(genericReminderRequest)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingProgressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
            binding.btnGoogle.isEnabled = false
        } else {
            binding.loadingProgressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.btnGoogle.isEnabled = true
        }
    }


    private suspend fun fetchAndSyncData(uid: String) {
        withContext(Dispatchers.IO) {
            Log.d("SYNC_DEBUG", "Starting data fetch for UID: $uid")
            try {
                // --- THIS IS THE FIX ---
                val userDao = SmartCampusApp.getDatabase().userDao()
                val timetableDao = SmartCampusApp.getDatabase().timetableDao()
                // -----------------------

                val userDocument = firestore.collection("users").document(uid).get().await()

                if (userDocument.exists()) {
                    val userName = userDocument.getString("userName") ?: "Student"
                    val photoUrl = userDocument.getString("photoUrl") ?: ""
                    val userClass = userDocument.getString("userClass") ?: ""
                    val userProfile = UserProfile(userName = userName, photoUrl = photoUrl)
                    userDao.insertOrUpdateUser(userProfile)

                    if (userClass.isNotEmpty()) {
                        val timetableDocument = firestore.collection("timetables").document(userClass).get().await()
                        if (timetableDocument.exists()) {
                            val scheduleList = timetableDocument.get("schedule").let {
                                if (it is List<*>) it.filterIsInstance<HashMap<String, String>>() else emptyList()
                            }

                            val timetableEntries = scheduleList.map {
                                TimetableEntry(
                                    subjectName = it["subject"] ?: "",
                                    timeSlot = it["time"] ?: "",
                                    location = it["location"] ?: "",
                                    dayOfWeek = it["day"] ?: ""
                                )
                            }
                            timetableDao.clearTable()
                            timetableDao.insertAll(timetableEntries)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "An error occurred during sync: ${e.message}", e)
            }
        }
    }
}