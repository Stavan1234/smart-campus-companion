package com.example.smartcampus.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartcampus.DashboardActivity
import com.example.smartcampus.LoginActivity
import com.example.smartcampus.OnboardingActivity
import com.example.smartcampus.R
import com.example.smartcampus.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Fancy loading messages
    private val loadingMessages = listOf(
        "Loading resources...",
        "Connecting to campus network...",
        "Initializing smart services...",
        "Waking up the AI bot...",
        "Almost there..."
    )

    // Config
    private val totalSplashMs = 5000L
    private val loadingMessageIntervalMs = 900L

    // coroutine job for updating text (will be cancelled automatically on destroy)
    private var textJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startSplashAnimations()
        startLoadingTextLoop()
        scheduleNavigationAfterDelay()
    }

    private fun startLoadingTextLoop() {
        // Start a coroutine that cycles through messages until cancelled
        textJob = lifecycleScope.launch {
            var idx = 0
            // Loop frequently until the coroutine is cancelled
            while (isActive) {
                binding.loadingTextView.text = loadingMessages[idx % loadingMessages.size]
                idx++
                delay(loadingMessageIntervalMs)
            }
        }
    }

    private fun startSplashAnimations() {
        // Entry animations (XML)
        val logoScaleIn = AnimationUtils.loadAnimation(this, R.anim.logo_scale_in)
        binding.logoImageView.startAnimation(logoScaleIn)

        val textSlideUp = AnimationUtils.loadAnimation(this, R.anim.text_slide_up)
        binding.appNameTextView.startAnimation(textSlideUp)

        // Stagger tagline slightly
        binding.taglineTextView.postDelayed({
            binding.taglineTextView.startAnimation(textSlideUp)
        }, 200)

        // Loading rotate
        val loadingRotate = AnimationUtils.loadAnimation(this, R.anim.loading_rotate)
        binding.loadingProgressBar.startAnimation(loadingRotate)

        // Particles: stagger their start times (use posts on the view so we can cancel if needed)
        val particleFloat = AnimationUtils.loadAnimation(this, R.anim.particle_float)
        binding.particle1.startAnimation(particleFloat)
        binding.particle2.postDelayed({ binding.particle2.startAnimation(particleFloat) }, 500)
        binding.particle3.postDelayed({ binding.particle3.startAnimation(particleFloat) }, 1000)

        // Background fade
        val backgroundFade = AnimationUtils.loadAnimation(this, R.anim.background_fade)
        binding.root.startAnimation(backgroundFade)
    }

    private fun scheduleNavigationAfterDelay() {
        // Use lifecycleScope to wait then animate exit and navigate
        lifecycleScope.launch {
            delay(totalSplashMs)

            // Cancel text loop before exit animation
            textJob?.cancel()

            // Decide target activity
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isOnboardingComplete = prefs.getBoolean("onboarding_complete", false)
            val currentUser = FirebaseAuth.getInstance().currentUser

            val targetActivity = when {
                currentUser != null -> DashboardActivity::class.java
                isOnboardingComplete -> LoginActivity::class.java
                else -> OnboardingActivity::class.java
            }

            // Exit fade animation via AnimatorSet
            val exitAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.logoImageView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.appNameTextView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.taglineTextView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.loadingProgressBar, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.loadingTextView, "alpha", 1f, 0f)
                )
                duration = 500
                interpolator = DecelerateInterpolator()
            }

            exitAnimator.start()

            // Wait for exit animation to finish
            delay(500)

            // Start next screen (check finishing state)
            if (!isFinishing) {
                startActivity(Intent(this@SplashActivity, targetActivity))
                finish()
                // Optionally: overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel coroutine job explicitly (safe even if already cancelled)
        textJob?.cancel()

        // Clear animations to avoid leaks and keep views clean
        binding.logoImageView.clearAnimation()
        binding.appNameTextView.clearAnimation()
        binding.taglineTextView.clearAnimation()
        binding.loadingProgressBar.clearAnimation()
        binding.particle1.clearAnimation()
        binding.particle2.clearAnimation()
        binding.particle3.clearAnimation()
        binding.root.clearAnimation()
    }
}
