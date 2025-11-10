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

    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val items = listOf(

            OnboardingItem(R.drawable.onb_screen_1, "Welcome to Our College", "A modern campus for your bright future."),
            OnboardingItem(R.drawable.onb_screen_2, "Stay Organized", "Get notifications and manage your calendar easily."),
            OnboardingItem(R.drawable.onb_screen_3, "Navigate with Ease", "Find your way with the interactive campus map."),
            OnboardingItem(R.drawable.onb_screen_4, "Smart Assistant", "Ask our AI bot for quick help anytime.")
        )

        adapter = OnboardingAdapter(items)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val dots = findViewById<DotsIndicator>(R.id.dotsIndicator)

        viewPager.adapter = adapter
        dots.attachTo(viewPager)

        // Next button logic
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < items.size) {
                viewPager.currentItem += 1
            } else {
                goToMain()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == items.lastIndex) {
                    btnNext.visibility = View.GONE
                    btnGetStarted.visibility = View.VISIBLE
                } else {
                    btnNext.visibility = View.VISIBLE
                    btnGetStarted.visibility = View.GONE
                }
            }
        })

        btnGetStarted.setOnClickListener { goToMain() }
    }

    private fun goToMain() {

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
