package com.example.smartcampus // Make sure this package name is correct

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartcampus.TimetableEntry
import com.example.smartcampus.UserProfile
import com.example.smartcampus.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.smartcampus.seed.SeedEvents
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
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // 1. Sync user/timetable data
                    fetchAndSyncData(firebaseUser.uid)

                    // 2. Seed events (runs only once, safe to leave in)
                    withContext(Dispatchers.IO) {
                        SeedEvents.seedEventsIfNeeded()
                    }

                    // 3. Move to Dashboard
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun fetchAndSyncData(uid: String) {
        // This function runs on a background thread because of Dispatchers.IO
        withContext(Dispatchers.IO) {
            Log.d("SYNC_DEBUG", "Starting data fetch for UID: $uid")
            try {
                // Get access to our local database DAOs
                val userDao = SmartCampusApp.database.userDao()
                val timetableDao = SmartCampusApp.database.timetableDao()

                // Fetch User Profile from Firestore
                Log.d("SYNC_DEBUG", "Fetching user document from 'users' collection with ID: $uid")
                val userDocument = firestore.collection("users").document(uid).get().await()

                if (userDocument.exists()) {
                    Log.d("SYNC_DEBUG", "SUCCESS: User document found!")
                    val userName = userDocument.getString("userName") ?: "Student"
                    val photoUrl = userDocument.getString("photoUrl") ?: ""
                    val userClass = userDocument.getString("userClass") ?: ""
                    Log.d("SYNC_DEBUG", "User Parsed: Name='${userName}', Class='${userClass}'")

                    val userProfile = UserProfile(userName = userName, photoUrl = photoUrl)
                    userDao.insertOrUpdateUser(userProfile)
                    Log.d("SYNC_DEBUG", "SUCCESS: User profile saved to Room.")


                    if (userClass.isNotEmpty()) {
                        Log.d("SYNC_DEBUG", "Fetching timetable from 'timetables' collection with ID: $userClass")
                        val timetableDocument = firestore.collection("timetables").document(userClass).get().await()

                        if (timetableDocument.exists()) {
                            Log.d("SYNC_DEBUG", "SUCCESS: Timetable document found!")
                            val scheduleList = timetableDocument.get("schedule") as? List<HashMap<String, String>> ?: emptyList()
                            Log.d("SYNC_DEBUG", "Timetable Parsed: Found ${scheduleList.size} entries.")

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
                            Log.d("SYNC_DEBUG", "SUCCESS: Timetable saved to Room.")
                        } else {
                            Log.e("SYNC_DEBUG", "ERROR: Timetable document '$userClass' does not exist!")
                        }
                    }
                } else {
                    Log.e("SYNC_DEBUG", "ERROR: User document with ID '$uid' does not exist in 'users' collection!")
                }

            } catch (e: Exception) {
                // This will catch any other errors during the process
                Log.e("SYNC_DEBUG", "An error occurred during sync: ${e.message}", e)
            }
        }
    }
}