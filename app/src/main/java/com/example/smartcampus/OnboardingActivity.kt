package com.example.smartcampus

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import android.content.Intent


class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var btnNext: Button
    private lateinit var btnGetStarted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // 1. Initialize Views
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnGetStarted = findViewById(R.id.btnGetStarted)

        // 2. Setup Adapter with data
        val onboardingItems = listOf(
            OnboardingItem(R.drawable.onb_screen_1, "Welcome to Our College", "A modern campus for your bright future."),
            OnboardingItem(R.drawable.onb_screen_2, "Stay Organized", "Get notifications and manage your calendar easily."),
            OnboardingItem(R.drawable.onb_screen_3, "Navigate with Ease", "Find your way with the interactive campus map."),
            OnboardingItem(R.drawable.onb_screen_4, "Smart Assistant", "Ask our AI bot for quick help anytime.")
        )

        onboardingAdapter = OnboardingAdapter(onboardingItems) {
            Toast.makeText(this, "Get Started Clicked!", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(this, MainActivity::class.java))
            // finish()
        }

        viewPager.adapter = onboardingAdapter

        // 3. Setup Dots Indicator
        val dotsIndicator = findViewById<DotsIndicator>(R.id.dotsIndicator)
        dotsIndicator.attachTo(viewPager)

        // 4. Page change listener to switch buttons
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingItems.lastIndex) {
                    btnNext.visibility = View.GONE
                    btnGetStarted.visibility = View.VISIBLE
                } else {
                    btnNext.visibility = View.VISIBLE
                    btnGetStarted.visibility = View.GONE
                }
            }
        })

        // 5. Button Clicks
        btnNext.setOnClickListener {
            viewPager.currentItem = viewPager.currentItem + 1
        }

        btnGetStarted.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // so onboarding doesn’t come back when pressing Back
        }
    }
}
