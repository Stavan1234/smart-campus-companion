//package com.example.smartcampus.ui.splash
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.example.smartcampus.MainActivity
//import com.example.smartcampus.databinding.ActivitySplashBinding
//
//class SplashActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivitySplashBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Inflate splash layout with ViewBinding
//        binding = ActivitySplashBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // When "Get Started" button is clicked → go to MainActivity
//        binding.btnGetStarted.setOnClickListener {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }
//    }
//}

package com.example.smartcampus.ui.splash // Make sure this package name is correct

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcampus.MainActivity
import com.example.smartcampus.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    // This variable will hold all the views from your XML layout
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This replaces setContentView() and prepares all your views
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Now you can access your button with 'binding.getStartedButton'
        // It's safer and cleaner than findViewById()
        binding.btnGetStarted.setOnClickListener {
            // Since you have no login yet, we will go to the main dashboard screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Closes the splash screen so the user can't go back to it
        }
    }
}