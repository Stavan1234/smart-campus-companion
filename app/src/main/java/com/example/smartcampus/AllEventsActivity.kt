package com.example.smartcampus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcampus.databinding.ActivityAllEventsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AllEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllEventsBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        fetchAllEvents()
    }

    private fun setupRecyclerView() {
        binding.allEventsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchAllEvents() {
        firestore.collection("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val eventList = snapshot.toObjects(Event::class.java)
                binding.allEventsRecyclerView.adapter = EventsAdapter(eventList)
            }
            .addOnFailureListener { e ->
                // Handle any errors, e.g., show a toast
            }
    }
}