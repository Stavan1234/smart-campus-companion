# 🎓 Smart Campus Companion

An all-in-one Android application designed to be the "saviour" for a student's chaotic college life. This app centralizes every scattered piece of campus information into one smart, intuitive, and predictive companion.

-----

## 🚀 The Problem We Solve

College life is confusing. Information is everywhere:

  * Timetables are in a PDF.
  * Event notices are on a physical board.
  * Campus maps are non-existent or hard to read.
  * You're constantly asking, "Where is room AX-510?" or "Will I be late for my next class?"

This disorganization leads to stress, being late, and missing out on campus life.

## ✨ The Solution: Your Campus, Simplified

The **Smart Campus Companion** connects everything. It's an intelligent helper that syncs live data from the cloud, works offline, and even predicts problems *before* they happen.

-----

## 📸 App Gallery

![dashboard](https://github.com/user-attachments/assets/4c9ee876-7a3b-446a-8297-ea6941a46c50)

![dashboard 1](https://github.com/user-attachments/assets/c5b46b53-5b30-4c39-b4c7-89fd2f0bad20)

![events](https://github.com/user-attachments/assets/53c5ccda-bcc8-4f36-8344-fe66935ffe49)

![event_1](https://github.com/user-attachments/assets/a183b436-1a22-43ab-929f-1fd10db281e2)

![chatbot](https://github.com/user-attachments/assets/3036a214-e68a-4797-b5d0-e9b0b8a12076)

![timetable](https://github.com/user-attachments/assets/4f661b37-482c-4497-aec3-c075ff083f39)

![map](https://github.com/user-attachments/assets/88ba6a67-0a00-437b-9dde-0f4475acfde4)


-----

## 🌟 Core Features

  * **Smart Dashboard:** The first screen you see. It greets you by name, shows your profile picture, and displays your live schedule for *today*.
  * 🧠 **Predictive ML Smart Alert:** The app's "brain." It's not just a simple check.
    1.  It parses your timetable for back-to-back classes.
    2.  It uses a **distance matrix** to calculate the *meters* you have to travel (e.g., "AX411" to "AX508").
    3.  It calculates the *time gap* you have in minutes.
    4.  It runs a **heuristic ML model** to predict your risk of being late, classifying it as `High Risk` (in red) or `Heads Up` (in blue).
  * ⚡ **Offline-First Timetable:** Your entire weekly timetable is synced from Firebase and stored locally in a **Room Database**. This means your schedule *always* loads instantly, even with bad campus Wi-Fi.
  * ⭐ **Interactive Canteen Menu:**
      * View the full canteen menu, organized by category.
      * Submit a **5-star rating** for any item.
      * All ratings are averaged and saved in **Firestore**, so the whole campus can see what's good.
  * 🗺️ **Live Campus Map:**
      * Full Google Maps integration with a custom map overlay.
      * Search bar to find any building, lab, or office.
      * A "My Location" button to find yourself when you're lost.
  * 🤖 **On-Device AI Chatbot:**
      * An instant, on-device chatbot for fast answers.
      * It's a deterministic AI, which means it's **100% accurate** for campus-specific info (no "hallucinations").
      * Features deep native integration—it can provide a button to **"View on Map"** that opens the map and pins the location.
  * 🗓️ **Campus Events:** A live-updating list of campus events, pulled directly from Firestore so you never miss a notice.
  * 🔒 **Secure Firebase Authentication:** Secure login and registration using Firebase Auth, with all user data synced to the cloud.

-----

## 🛠️ Tech Stack & Architecture

This project is built using modern, professional Android development standards.

  * **Language:** 100% **Kotlin**
  * **Architecture:** MVVM-inspired (Activities/Fragments + Lifecycle)
  * **Core:** Kotlin Coroutines, WorkManager (for background sync)
  * **UI:** Material Design 3, ViewBinding, Glide (for image loading)
  * **Remote Database:** **Cloud Firestore** (for timetables, events, locations, ratings)
  * **Local Database:** **Room** (for caching user profile & timetable offline)
  * **Services:** Google Maps SDK, Firebase Authentication
  * **Build:** Gradle with Kotlin KTS

-----

## 🏃 How to Run This Project

To build and run this project yourself, you need to provide two secret files:

1.  **Firebase Config:**

      * Go to your new Firebase project.
      * Create an Android app with the package name `com.example.smartcampus`.
      * Download the `google-services.json` file.
      * Place this file in the `app/` directory.

2.  **Google Maps API Key:**

      * This project securely loads the API key from a `local.properties` file (which is *not* on GitHub).
      * In the root of the project, create a file named `local.properties`.
      * Go to your Google Cloud Console, get your Maps API Key, and paste it into the file like this:
        ```
        MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
        ```
      * Build the project. The `build.gradle.kts` file will automatically read this key and inject it into the app.

-----

## 👨‍💻 Authors

  * **Stavan Kalkumbe**
  * **Mohammed Faheem Madhia**
