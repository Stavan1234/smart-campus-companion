package com.example.smartcampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.smartcampus.databinding.ActivityEventDetailBinding
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Add smooth entrance animation
        startEntranceAnimation()

        val eventId = intent.getStringExtra("EVENT_ID")
        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        fetchEventDetails(eventId)
        setupClickListeners()
    }

    private fun fetchEventDetails(eventId: String) {
        firestore.collection("events").document(eventId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val event = document.toObject<EventDetail>()
                    if (event != null) {
                        populateUi(event)
                    }
                } else {
                    Toast.makeText(this, "Error: Event not found.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting event details: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun populateUi(event: EventDetail) {
        binding.collapsingToolbarLayout.title = event.title
        binding.eventDetailTitle.text = event.title
        binding.eventDetailDate.text = "🗓️  ${event.date}"
        binding.eventDetailVenue.text = "📍  ${event.venue}"
        binding.eventDetailOrganizer.text = "👥  ${event.organizer}"
        binding.eventDetailDescription.text = event.description

        Glide.with(this).load(event.imageUrl).into(binding.eventDetailImage)

        binding.eventDetailTags.removeAllViews()
        event.tags.forEach { tagName ->
            val chip = Chip(this)
            chip.text = tagName
            binding.eventDetailTags.addView(chip)
        }

        if (event.videoUrl.isNotBlank()) {
            binding.videoHeader.visibility = View.VISIBLE
            binding.videoPlayerView.visibility = View.VISIBLE
            initializePlayer(event.videoUrl)
        } else {
            binding.videoHeader.visibility = View.GONE
            binding.videoPlayerView.visibility = View.GONE
        }

        // Add staggered animation to content
        startContentAnimation()
    }

    private fun initializePlayer(videoUrl: String) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.videoPlayerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            exoPlayer.release()
            player = null
        }
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            // Add button animation
            binding.registerButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    binding.registerButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            Toast.makeText(this, "Registration feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.whatsappIcon.setOnClickListener {
            openContactLink("https://wa.me/919876543210")
        }

        binding.instagramIcon.setOnClickListener {
            openContactLink("https://instagram.com/college_events")
        }

        binding.discordIcon.setOnClickListener {
            openContactLink("https://discord.gg/college-events")
        }
    }

    private fun openContactLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startEntranceAnimation() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.appBar.startAnimation(slideUp)
    }

    private fun startContentAnimation() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        
        binding.eventDetailTitle.startAnimation(fadeIn)
        binding.eventDetailTags.postDelayed({ binding.eventDetailTags.startAnimation(fadeIn) }, 100)
        binding.registerButton.postDelayed({ binding.registerButton.startAnimation(fadeIn) }, 200)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}