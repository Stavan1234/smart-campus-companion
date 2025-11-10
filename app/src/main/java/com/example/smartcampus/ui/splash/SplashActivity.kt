package com.example.smartcampus.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcampus.DashboardActivity
import com.example.smartcampus.LoginActivity
import com.example.smartcampus.OnboardingActivity
import com.example.smartcampus.R
import com.example.smartcampus.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start all animations
        startSplashAnimations()

        // Navigate after delay
        navigateAfterDelay()
    }

    private fun startSplashAnimations() {
        // Logo animation with scale and fade
        val logoScaleIn = AnimationUtils.loadAnimation(this, R.anim.logo_scale_in)
        binding.logoImageView.startAnimation(logoScaleIn)

        // App name slide up animation
        val textSlideUp = AnimationUtils.loadAnimation(this, R.anim.text_slide_up)
        binding.appNameTextView.startAnimation(textSlideUp)

        // Tagline animation with slight delay
        Handler(Looper.getMainLooper()).postDelayed({
            val taglineSlideUp = AnimationUtils.loadAnimation(this, R.anim.text_slide_up)
            binding.taglineTextView.startAnimation(taglineSlideUp)
        }, 200)

        // Loading indicator rotation
        val loadingRotate = AnimationUtils.loadAnimation(this, R.anim.loading_rotate)
        binding.loadingProgressBar.startAnimation(loadingRotate)

        // Background particles floating animation
        val particleFloat = AnimationUtils.loadAnimation(this, R.anim.particle_float)
        binding.particle1.startAnimation(particleFloat)
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.particle2.startAnimation(particleFloat)
        }, 500)
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.particle3.startAnimation(particleFloat)
        }, 1000)

        // Background fade animation
        val backgroundFade = AnimationUtils.loadAnimation(this, R.anim.background_fade)
        binding.root.startAnimation(backgroundFade)
    }

    private fun navigateAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user has completed onboarding
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isOnboardingComplete = prefs.getBoolean("onboarding_complete", false)

            // Check if a user is already logged in
            val currentUser = FirebaseAuth.getInstance().currentUser

            // Decide which screen to go to
            val targetActivity = when {
                currentUser != null -> {
                    // If user is logged in, go straight to the Dashboard
                    DashboardActivity::class.java
                }
                isOnboardingComplete -> {
                    // If onboarding is done but not logged in, go to Login
                    LoginActivity::class.java
                }
                else -> {
                    // If it's the very first time, show Onboarding
                    OnboardingActivity::class.java
                }
            }

            // Add exit animation before navigation
            val exitAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.logoImageView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.appNameTextView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.taglineTextView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(binding.loadingProgressBar, "alpha", 1f, 0f)
                )
                duration = 500
                interpolator = DecelerateInterpolator()
            }
            
            exitAnimator.start()
            
            // Navigate after exit animation
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, targetActivity))
                finish()
            }, 500)

        }, 5000) // 5 second delay for full animation sequence
    }
}