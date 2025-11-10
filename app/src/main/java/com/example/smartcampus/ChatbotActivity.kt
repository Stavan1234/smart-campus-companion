package com.example.smartcampus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcampus.databinding.ActivityChatbotBinding
import com.google.firebase.firestore.FirebaseFirestore

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var botBrain: CampusBotBrain

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup the RecyclerView and Adapter
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecyclerView.adapter = chatAdapter

        // Initialize the bot's "brain" and tell it what to do when it has a response
        botBrain = CampusBotBrain(FirebaseFirestore.getInstance()) { responseText, locationName ->
            addBotMessage(responseText, locationName)
        }

        // Handle the send button click
        binding.sendButton.setOnClickListener {
            val userInput = binding.messageEditText.text.toString()
            if (userInput.isNotBlank()) {
                addUserMessage(userInput)
                botBrain.process(userInput) // Send the user's input to the brain
                binding.messageEditText.text.clear()
            }
        }

        // Add the initial welcome message
        addBotMessage("👋 Hello! I'm your SmartCampus Assistant.\nAsk me about any location, timing, or event.")
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String, location: String? = null) {
        messages.add(ChatMessage(text, false, location))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }
}